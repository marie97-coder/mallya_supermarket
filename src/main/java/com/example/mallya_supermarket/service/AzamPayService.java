package com.example.mallya_supermarket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AzamPayService {

    private final ObjectMapper objectMapper;

    @Value("${payment.azampay.enabled:false}")
    private boolean enabled;

    @Value("${payment.azampay.auth-base-url:https://authenticator-sandbox.azampay.co.tz}")
    private String authBaseUrl;

    @Value("${payment.azampay.checkout-base-url:https://sandbox.azampay.co.tz}")
    private String checkoutBaseUrl;

    @Value("${payment.azampay.app-name:}")
    private String appName;

    @Value("${payment.azampay.client-id:}")
    private String clientId;

    @Value("${payment.azampay.client-secret:}")
    private String clientSecret;

    @Value("${payment.azampay.default-currency:TZS}")
    private String defaultCurrency;

    @Value("${payment.azampay.timeout-seconds:25}")
    private long timeoutSeconds;

    @Value("${payment.azampay.connect-timeout-seconds:0}")
    private long connectTimeoutSeconds;

    @Value("${payment.azampay.request-timeout-seconds:0}")
    private long requestTimeoutSeconds;

    private HttpClient httpClient;

    @PostConstruct
    void initHttpClient() {
        long effectiveTimeout = resolveTimeoutSeconds(connectTimeoutSeconds);
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(effectiveTimeout))
            .build();
    }

    public AzamPayCheckoutResult checkoutMobileMoney(BigDecimal amount,
                                                     String accountNumber,
                                                     String provider,
                                                     String externalId) {
        ensureConfigured();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (isBlank(accountNumber)) {
            throw new IllegalArgumentException("Customer mobile number is required");
        }
        if (isBlank(provider)) {
            throw new IllegalArgumentException("AzamPay provider is required");
        }
        if (isBlank(externalId)) {
            throw new IllegalArgumentException("External ID is required");
        }

        String bearerToken = generateBearerToken();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "mno");
        payload.put("provider", normalizeProvider(provider));
        payload.put("amount", normalizeAmount(amount));
        payload.put("currency", defaultCurrency);
        payload.put("accountNumber", accountNumber.trim());
        payload.put("externalId", externalId.trim());
        payload.put("additionalProperties", Map.of());

        JsonNode response = postJson(
            normalizedUrl(checkoutBaseUrl) + "/api/v1/checkout/checkoutmno",
            payload,
            Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
        );

        boolean success = response.path("success").asBoolean(false);
        String message = firstNonBlank(
            response.path("message").asText(null),
            response.path("data").path("message").asText(null),
            "AzamPay request failed"
        );
        String transactionId = firstNonBlank(
            response.path("transactionId").asText(null),
            response.path("data").path("transactionId").asText(null),
            response.path("data").path("referenceId").asText(null),
            externalId.trim()
        );

        return new AzamPayCheckoutResult(success, transactionId, message, response);
    }

    public Map<String, Object> checkConnectivity() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("authBaseUrl", authBaseUrl);
        status.put("checkoutBaseUrl", checkoutBaseUrl);
        status.put("timestamp", LocalDateTime.now());

        try {
            generateBearerToken();
            status.put("reachable", true);
            status.put("message", "AzamPay auth token acquired");
        } catch (Exception ex) {
            status.put("reachable", false);
            status.put("message", ex.getMessage());
        }

        return status;
    }

    private String generateBearerToken() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appName", appName.trim());
        payload.put("clientId", clientId.trim());
        payload.put("clientSecret", clientSecret.trim());

        JsonNode response = postJson(
            normalizedUrl(authBaseUrl) + "/AppRegistration/GenerateToken",
            payload,
            Map.of()
        );

        boolean success = response.path("success").asBoolean(true);
        if (!success) {
            throw new IllegalStateException(firstNonBlank(
                response.path("message").asText(null),
                "AzamPay token request failed"
            ));
        }

        String token = firstNonBlank(
            response.path("data").path("token").asText(null),
            response.path("data").path("accessToken").asText(null),
            response.path("data").path("jwt").asText(null),
            response.path("token").asText(null),
            response.path("accessToken").asText(null)
        );

        if (isBlank(token)) {
            throw new IllegalStateException("AzamPay token missing in token response");
        }

        return token.trim();
    }

    private JsonNode postJson(String url, Map<String, Object> payload, Map<String, String> extraHeaders) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            Duration requestTimeout = Duration.ofSeconds(resolveTimeoutSeconds(requestTimeoutSeconds));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body));

            for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }

            log.debug("AzamPay request: url={}, timeout={}s", url, requestTimeout.getSeconds());
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JsonNode json;
            try {
                json = parseJson(responseBody);
            } catch (JsonProcessingException ex) {
                log.error("AzamPay returned non-JSON response (status {}): {}", response.statusCode(), responseBody);
                throw new IllegalStateException("Invalid response from AzamPay");
            }

            if (response.statusCode() / 100 != 2) {
                String message = firstNonBlank(
                    json.path("message").asText(null),
                    json.path("data").path("message").asText(null),
                    "AzamPay request failed with status " + response.statusCode()
                );
                log.error("AzamPay request failed (status {}): {}", response.statusCode(), responseBody);
                throw new IllegalStateException(message);
            }

            log.debug("AzamPay response: status={}, body={}", response.statusCode(), responseBody);
            return json;
        } catch (HttpTimeoutException ex) {
            log.error("AzamPay request timed out for {}", url);
            throw new IllegalStateException("AzamPay request timed out. Check connectivity or increase timeout.");
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize AzamPay payload", ex);
            throw new IllegalStateException("Failed to prepare AzamPay request");
        } catch (IOException ex) {
            log.error("Failed to parse AzamPay response", ex);
            throw new IllegalStateException("Invalid response from AzamPay");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AzamPay request interrupted");
        }
    }

    private JsonNode parseJson(String body) throws JsonProcessingException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private String normalizeProvider(String provider) {
        return provider.trim().toLowerCase();
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private long resolveTimeoutSeconds(long overrideSeconds) {
        long fallback = timeoutSeconds > 0 ? timeoutSeconds : 25;
        return overrideSeconds > 0 ? overrideSeconds : fallback;
    }

    private String normalizedUrl(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private void ensureConfigured() {
        if (!enabled) {
            throw new IllegalStateException("AzamPay payment is disabled");
        }
        if (isBlank(appName) || isBlank(clientId) || isBlank(clientSecret)) {
            throw new IllegalStateException("AzamPay credentials are not configured");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record AzamPayCheckoutResult(
        boolean success,
        String transactionId,
        String message,
        JsonNode rawResponse
    ) {}
}
