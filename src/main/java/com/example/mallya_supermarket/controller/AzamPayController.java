package com.example.mallya_supermarket.controller;

import com.example.mallya_supermarket.dto.SaleItemRequest;
import com.example.mallya_supermarket.entity.PaymentMethod;
import com.example.mallya_supermarket.entity.Sale;
import com.example.mallya_supermarket.entity.SaleItem;
import com.example.mallya_supermarket.service.AzamPayService;
import com.example.mallya_supermarket.service.POSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/azampay")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AzamPayController {

    private final POSService posService;
    private final AzamPayService azamPayService;

    @PostMapping("/checkout-mno")
    public ResponseEntity<?> checkoutMobileMoney(@RequestBody AzamPayCheckoutRequest request) {
        try {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body("Sale items are required");
            }
            if (request.getCashierUsername() == null || request.getCashierUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Cashier username is required");
            }
            if (request.getProvider() == null || request.getProvider().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("AzamPay provider is required");
            }
            if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Customer mobile number is required");
            }

            List<SaleItemRequest> saleItems = request.getItems().stream()
                .map(item -> new SaleItemRequest(item.getBarcode(), item.getQuantity()))
                .toList();

            BigDecimal saleTotal = posService.calculateSaleTotal(saleItems);
            String externalId = resolveExternalId(request.getExternalId());

            AzamPayService.AzamPayCheckoutResult paymentResult = azamPayService.checkoutMobileMoney(
                saleTotal,
                request.getAccountNumber(),
                request.getProvider(),
                externalId
            );

            if (!paymentResult.success()) {
                return ResponseEntity.badRequest().body(paymentResult.message());
            }

            Sale sale = posService.completeSale(
                saleItems,
                request.getCashierUsername().trim(),
                PaymentMethod.MOBILE_MONEY,
                saleTotal,
                paymentResult.transactionId()
            );

            Map<String, Object> receipt = toSaleReceipt(sale);
            receipt.put("paymentGateway", "AZAMPAY");
            receipt.put("gatewayMessage", paymentResult.message());
            receipt.put("externalId", externalId);
            receipt.put("azamPayTransactionId", paymentResult.transactionId());

            return ResponseEntity.ok(receipt);
        } catch (Exception ex) {
            log.error("AzamPay checkout failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> checkAzamPayConnectivity() {
        try {
            return ResponseEntity.ok(azamPayService.checkConnectivity());
        } catch (Exception ex) {
            log.error("AzamPay health check failed: {}", ex.getMessage());
            return ResponseEntity.internalServerError().body("AzamPay health check failed");
        }
    }

    private String resolveExternalId(String requestedExternalId) {
        if (requestedExternalId != null && !requestedExternalId.trim().isEmpty()) {
            return requestedExternalId.trim();
        }
        return "sale-" + UUID.randomUUID();
    }

    private Map<String, Object> toSaleReceipt(Sale sale) {
        BigDecimal tax = sale.getTax() == null ? BigDecimal.ZERO : sale.getTax();
        BigDecimal total = sale.getTotalPrice() == null ? BigDecimal.ZERO : sale.getTotalPrice();
        BigDecimal subtotal = total.subtract(tax);

        List<Map<String, Object>> items = sale.getSaleItems().stream()
            .map(this::toReceiptItem)
            .toList();

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("saleId", sale.getId());
        receipt.put("timestamp", sale.getTimestamp());
        receipt.put("cashier", sale.getCashier().getUsername());
        receipt.put("subtotal", subtotal);
        receipt.put("tax", tax);
        receipt.put("total", total);
        receipt.put("paymentMethod", sale.getPaymentMethod() == null ? null : sale.getPaymentMethod().name());
        receipt.put("amountReceived", sale.getAmountReceived());
        receipt.put("changeAmount", sale.getChangeAmount());
        receipt.put("transactionReference", sale.getTransactionReference());
        receipt.put("items", items);
        return receipt;
    }

    private Map<String, Object> toReceiptItem(SaleItem saleItem) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("barcode", saleItem.getProduct().getBarcode());
        item.put("name", saleItem.getProduct().getName());
        item.put("quantity", saleItem.getQuantity());
        item.put("unitPrice", saleItem.getPriceAtSale());
        item.put("lineTotal", saleItem.getPriceAtSale().multiply(BigDecimal.valueOf(saleItem.getQuantity())));
        return item;
    }

    public static class AzamPayCheckoutRequest {
        private List<ItemRequest> items;
        private String cashierUsername;
        private String provider;
        private String accountNumber;
        private String externalId;

        public List<ItemRequest> getItems() {
            return items;
        }

        public void setItems(List<ItemRequest> items) {
            this.items = items;
        }

        public String getCashierUsername() {
            return cashierUsername;
        }

        public void setCashierUsername(String cashierUsername) {
            this.cashierUsername = cashierUsername;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }
    }

    public static class ItemRequest {
        private String barcode;
        private Integer quantity;

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
