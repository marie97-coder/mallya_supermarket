package com.example.mallya_supermarket.controller;

import com.example.mallya_supermarket.service.AdminService;
import com.example.mallya_supermarket.service.ReportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@RequestBody AddProductRequest request) {
        try {
            return ResponseEntity.ok(adminService.addProduct(
                request.getBarcode(),
                request.getName(),
                request.getDescription(),
                request.getCategoryName(),
                request.getCostPrice(),
                request.getSellingPrice(),
                request.getStockQuantity(),
                request.getExpiryDate()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding product: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to add product");
        }
    }

    @PatchMapping("/products/{barcode}/price")
    public ResponseEntity<?> updateProductPrice(@PathVariable String barcode,
                                                @RequestBody UpdatePriceRequest request,
                                                Authentication authentication) {
        try {
            String username = authentication == null ? "system" : authentication.getName();
            return ResponseEntity.ok(adminService.updateProductPrice(barcode, request.getNewPrice(), username));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating product price: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to update price");
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                           @RequestBody UpdateProductRequest request,
                                           Authentication authentication) {
        try {
            String username = authentication == null ? "system" : authentication.getName();
            return ResponseEntity.ok(adminService.updateProduct(
                id,
                request.getBarcode(),
                request.getName(),
                request.getDescription(),
                request.getCategoryName(),
                request.getCostPrice(),
                request.getSellingPrice(),
                request.getStockQuantity(),
                request.getExpiryDate(),
                username
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating product: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to update product");
        }
    }

    @GetMapping("/reports/profit-loss")
    public ResponseEntity<?> getProfitLossReport(@RequestParam(defaultValue = "daily") String period) {
        try {
            return ResponseEntity.ok(reportService.getProfitLossReport(period));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error getting profit/loss report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to generate report");
        }
    }

    @GetMapping("/reports/product-activity")
    public ResponseEntity<?> getProductActivityReport(@RequestParam(defaultValue = "") String query) {
        try {
            return ResponseEntity.ok(reportService.getProductActivityReport(query));
        } catch (Exception e) {
            log.error("Error getting product activity report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to generate product activity report");
        }
    }

    @PostMapping("/cashiers")
    public ResponseEntity<?> registerCashier(@RequestBody RegisterCashierRequest request) {
        try {
            return ResponseEntity.ok(adminService.registerCashier(request.getUsername(), request.getPassword()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error registering cashier: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to register cashier");
        }
    }

    @GetMapping("/cashiers")
    public ResponseEntity<?> getCashiers() {
        try {
            return ResponseEntity.ok(adminService.getCashiers());
        } catch (Exception e) {
            log.error("Error loading cashiers: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to load cashiers");
        }
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProducts() {
        try {
            return ResponseEntity.ok(adminService.getProducts());
        } catch (Exception e) {
            log.error("Error loading products: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to load products");
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            adminService.deleteProduct(id);
            return ResponseEntity.ok("Product deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting product: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to delete product");
        }
    }

    @GetMapping("/sales")
    public ResponseEntity<?> getRecentSales(@RequestParam(defaultValue = "20") int limit) {
        try {
            return ResponseEntity.ok(adminService.getRecentSales(limit));
        } catch (Exception e) {
            log.error("Error loading sales: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to load sales");
        }
    }

    @GetMapping("/sales/{id}")
    public ResponseEntity<?> getSaleDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(adminService.getSaleDetails(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error loading sale details: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to load sale details");
        }
    }

    @PutMapping("/sales/{id}")
    public ResponseEntity<?> updateSale(@PathVariable Long id, @RequestBody UpdateSaleRequest request) {
        try {
            return ResponseEntity.ok(adminService.updateSale(
                id,
                request.getPaymentMethod(),
                request.getAmountReceived(),
                request.getTransactionReference()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating sale: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to update sale");
        }
    }

    @Data
    public static class AddProductRequest {
        private String barcode;
        private String name;
        private String description;
        private String categoryName;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private Integer stockQuantity;
        private String expiryDate;
    }

    @Data
    public static class UpdatePriceRequest {
        private BigDecimal newPrice;
    }

    @Data
    public static class UpdateProductRequest {
        private String barcode;
        private String name;
        private String description;
        private String categoryName;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private Integer stockQuantity;
        private String expiryDate;
    }

    @Data
    public static class UpdateSaleRequest {
        private String paymentMethod;
        private BigDecimal amountReceived;
        private String transactionReference;
    }

    @Data
    public static class RegisterCashierRequest {
        private String username;
        private String password;
    }
}
