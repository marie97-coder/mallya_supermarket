package com.example.mallya_supermarket.service;

import com.example.mallya_supermarket.entity.Product;
import com.example.mallya_supermarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpirationService {
    
    private final ProductRepository productRepository;
    
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateExpiredProducts() {
        log.info("Starting scheduled expiration check...");
        
        List<Product> products = productRepository.findActiveProducts();
        LocalDate today = LocalDate.now();
        
        int expiredCount = 0;
        for (Product product : products) {
            if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(today)) {
                product.setIsExpired(true);
                productRepository.save(product);
                expiredCount++;
            }
        }
        
        log.info("Expiration check completed. Marked {} products as expired.", expiredCount);
    }
    
    public List<Product> getProductsExpiringSoon() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        
        return productRepository.findProductsExpiringSoon(today, thirtyDaysFromNow);
    }
    
    public List<Product> getExpiredProducts() {
        return productRepository.findByIsExpiredTrue();
    }
    
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }
}
