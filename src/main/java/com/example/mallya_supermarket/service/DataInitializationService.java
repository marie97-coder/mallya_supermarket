package com.example.mallya_supermarket.service;

import com.example.mallya_supermarket.entity.Category;
import com.example.mallya_supermarket.entity.Product;
import com.example.mallya_supermarket.entity.User;
import com.example.mallya_supermarket.repository.CategoryRepository;
import com.example.mallya_supermarket.repository.ProductRepository;
import com.example.mallya_supermarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {
    
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        initializeData();
    }
    
    private void initializeData() {
        if (userRepository.count() == 0) {
            log.info("Initializing sample data...");
            
            // Create categories
            Category beverages = createCategory("Beverages", "Soft drinks, juices, water");
            Category dairy = createCategory("Dairy", "Milk, cheese, yogurt");
            Category snacks = createCategory("Snacks", "Chips, cookies, nuts");
            Category bakery = createCategory("Bakery", "Bread, pastries, cakes");
            
            // Create products
            createProduct("1234567890", "Coca Cola 500ml", beverages, new BigDecimal("1.50"), new BigDecimal("2.50"), 100, LocalDate.now().plusMonths(6));
            createProduct("1234567891", "Pepsi 500ml", beverages, new BigDecimal("1.45"), new BigDecimal("2.45"), 80, LocalDate.now().plusMonths(5));
            createProduct("1234567892", "Fresh Milk 1L", dairy, new BigDecimal("2.00"), new BigDecimal("3.50"), 50, LocalDate.now().plusDays(7));
            createProduct("1234567893", "Greek Yogurt", dairy, new BigDecimal("1.80"), new BigDecimal("3.20"), 30, LocalDate.now().plusDays(5));
            createProduct("1234567894", "Potato Chips", snacks, new BigDecimal("1.20"), new BigDecimal("2.00"), 200, LocalDate.now().plusMonths(8));
            createProduct("1234567895", "Chocolate Cookies", snacks, new BigDecimal("1.50"), new BigDecimal("2.80"), 60, LocalDate.now().plusMonths(4));
            createProduct("1234567896", "Whole Wheat Bread", bakery, new BigDecimal("1.00"), new BigDecimal("2.20"), 40, LocalDate.now().plusDays(3));
            createProduct("1234567897", "Croissants", bakery, new BigDecimal("0.80"), new BigDecimal("1.80"), 25, LocalDate.now().plusDays(2));
            
            // Create some expired products for testing
            createProduct("9999999991", "Expired Product 1", dairy, new BigDecimal("1.00"), new BigDecimal("2.00"), 5, LocalDate.now().minusDays(10));
            createProduct("9999999992", "Expired Product 2", snacks, new BigDecimal("1.20"), new BigDecimal("2.50"), 3, LocalDate.now().minusDays(5));
            
            // Create users
            createUser("admin", passwordEncoder.encode("admin123"), "ADMIN");
            createUser("cashier1", passwordEncoder.encode("cashier123"), "CASHIER");
            createUser("cashier2", passwordEncoder.encode("cashier456"), "CASHIER");
            
            log.info("Sample data initialized successfully!");
        }
    }
    
    private Category createCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }
    
    private void createProduct(String barcode, String name, Category category, BigDecimal costPrice, BigDecimal sellingPrice, int stockQuantity, LocalDate expiryDate) {
        Product product = new Product();
        product.setBarcode(barcode);
        product.setName(name);
        product.setCategory(category);
        product.setCostPrice(costPrice);
        product.setSellingPrice(sellingPrice);
        product.setStockQuantity(stockQuantity);
        product.setExpiryDate(expiryDate);
        product.setDescription("Sample product: " + name);
        
        // Check if product should be marked as expired
        if (expiryDate.isBefore(LocalDate.now())) {
            product.setIsExpired(true);
        }
        
        productRepository.save(product);
    }
    
    private void createUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        userRepository.save(user);
    }
}
