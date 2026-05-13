package com.example.mallya_supermarket.controller;

import com.example.mallya_supermarket.entity.Product;
import com.example.mallya_supermarket.service.ExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class InventoryController {
    
    private final ExpirationService expirationService;
    
    @GetMapping("/expiring-soon")
    public ResponseEntity<List<Product>> getProductsExpiringSoon() {
        try {
            List<Product> products = expirationService.getProductsExpiringSoon();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error fetching products expiring soon: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/expired")
    public ResponseEntity<List<Product>> getExpiredProducts() {
        try {
            List<Product> products = expirationService.getExpiredProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error fetching expired products: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(@RequestParam(defaultValue = "10") int threshold) {
        try {
            List<Product> products = expirationService.getLowStockProducts(threshold);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error fetching low stock products: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getInventoryAlerts() {
        try {
            List<Product> expiringSoon = expirationService.getProductsExpiringSoon();
            List<Product> expired = expirationService.getExpiredProducts();
            List<Product> lowStock = expirationService.getLowStockProducts(10);
            
            Map<String, Object> alerts = Map.of(
                "expiringSoon", expiringSoon,
                "expired", expired,
                "lowStock", lowStock,
                "expiringSoonCount", expiringSoon.size(),
                "expiredCount", expired.size(),
                "lowStockCount", lowStock.size()
            );
            
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching inventory alerts: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
