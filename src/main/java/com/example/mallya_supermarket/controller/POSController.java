package com.example.mallya_supermarket.controller;

import com.example.mallya_supermarket.dto.SaleItemRequest;
import com.example.mallya_supermarket.entity.PaymentMethod;
import com.example.mallya_supermarket.entity.Product;
import com.example.mallya_supermarket.entity.Sale;
import com.example.mallya_supermarket.entity.SaleItem;
import com.example.mallya_supermarket.service.POSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class POSController {
    
    private final POSService posService;

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String query) {
        try {
            List<Product> products = posService.searchProducts(query);
            return ResponseEntity.ok(products.stream().map(this::toProductSummary).toList());
        } catch (Exception e) {
            log.error("Error searching products: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error searching products");
        }
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanProduct(@RequestBody Map<String, String> request) {
        try {
            String barcode = request.get("barcode");
            if (barcode == null || barcode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Barcode is required");
            }
            
            Product product = posService.scanProduct(barcode.trim());
            return ResponseEntity.ok(toProductSummary(product));
            
        } catch (Exception e) {
            log.error("Error scanning product: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/sale")
    public ResponseEntity<?> completeSale(@RequestBody SaleRequest saleRequest) {
        try {
            if (saleRequest.getItems() == null || saleRequest.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body("Sale items are required");
            }
            
            if (saleRequest.getCashierUsername() == null || saleRequest.getCashierUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Cashier username is required");
            }
            
            List<SaleItemRequest> saleRequests = saleRequest.getItems().stream()
                    .map(item -> new SaleItemRequest(item.getBarcode(), item.getQuantity()))
                    .toList();

            PaymentMethod paymentMethod = parsePaymentMethod(saleRequest.getPaymentMethod());
            
            Sale sale = posService.completeSale(
                saleRequests,
                saleRequest.getCashierUsername(),
                paymentMethod,
                saleRequest.getAmountReceived(),
                saleRequest.getTransactionReference()
            );
            return ResponseEntity.ok(toSaleReceipt(sale));
            
        } catch (Exception e) {
            log.error("Error processing sale: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Map<String, Object> toProductSummary(Product product) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", product.getId());
        summary.put("barcode", product.getBarcode());
        summary.put("name", product.getName());
        summary.put("sellingPrice", product.getSellingPrice());
        summary.put("stockQuantity", product.getStockQuantity());
        summary.put("isExpired", product.getIsExpired());
        summary.put("categoryName", product.getCategory() != null ? product.getCategory().getName() : "");
        return summary;
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

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Payment method is required");
        }

        String normalized = value.trim().toUpperCase()
            .replace("-", "_")
            .replace(" ", "_");

        return switch (normalized) {
            case "CASH" -> PaymentMethod.CASH;
            case "MOBILE_MONEY", "MOBILEMONEY", "MPESA", "TIGOPESA" -> PaymentMethod.MOBILE_MONEY;
            case "CARD", "BANK_CARD", "BANKCARDS" -> PaymentMethod.CARD;
            default -> throw new RuntimeException("Unsupported payment method: " + value);
        };
    }
    
    public static class SaleRequest {
        private List<ItemRequest> items;
        private String cashierUsername;
        private String paymentMethod;
        private BigDecimal amountReceived;
        private String transactionReference;
        
        public List<ItemRequest> getItems() { return items; }
        public void setItems(List<ItemRequest> items) { this.items = items; }
        public String getCashierUsername() { return cashierUsername; }
        public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public BigDecimal getAmountReceived() { return amountReceived; }
        public void setAmountReceived(BigDecimal amountReceived) { this.amountReceived = amountReceived; }
        public String getTransactionReference() { return transactionReference; }
        public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    }
    
    public static class ItemRequest {
        private String barcode;
        private Integer quantity;
        
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
