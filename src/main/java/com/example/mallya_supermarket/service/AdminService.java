package com.example.mallya_supermarket.service;

import com.example.mallya_supermarket.entity.Category;
import com.example.mallya_supermarket.entity.PaymentMethod;
import com.example.mallya_supermarket.entity.PriceUpdateHistory;
import com.example.mallya_supermarket.entity.Product;
import com.example.mallya_supermarket.entity.Sale;
import com.example.mallya_supermarket.entity.SaleItem;
import com.example.mallya_supermarket.entity.User;
import com.example.mallya_supermarket.repository.CategoryRepository;
import com.example.mallya_supermarket.repository.PriceUpdateHistoryRepository;
import com.example.mallya_supermarket.repository.ProductRepository;
import com.example.mallya_supermarket.repository.SaleRepository;
import com.example.mallya_supermarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final PriceUpdateHistoryRepository priceUpdateHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, Object> addProduct(String barcode,
                                          String name,
                                          String description,
                                          String categoryName,
                                          BigDecimal costPrice,
                                          BigDecimal sellingPrice,
                                          Integer stockQuantity,
                                          String expiryDate) {

        validateRequiredText(barcode, "Barcode is required");
        validateRequiredText(name, "Product name is required");
        validateRequiredText(categoryName, "Category name is required");
        validateMoney(costPrice, "Cost price must be greater than zero");
        validateMoney(sellingPrice, "Selling price must be greater than zero");

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity must be zero or greater");
        }

        if (productRepository.existsByBarcode(barcode.trim())) {
            throw new IllegalArgumentException("Product with this barcode already exists");
        }

        Category category = categoryRepository.findByName(categoryName.trim())
            .orElseGet(() -> {
                Category newCategory = new Category();
                newCategory.setName(categoryName.trim());
                newCategory.setDescription("Created by admin dashboard");
                return categoryRepository.save(newCategory);
            });

        Product product = new Product();
        product.setBarcode(barcode.trim());
        product.setName(name.trim());
        product.setDescription(Optional.ofNullable(description).orElse("").trim());
        product.setCategory(category);
        product.setCostPrice(costPrice);
        product.setSellingPrice(sellingPrice);
        product.setStockQuantity(stockQuantity);
        product.setExpiryDate(parseDate(expiryDate));
        product.setDeleted(false);
        product.setDeletedAt(null);

        Product saved = productRepository.save(product);
        return toProductResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateProductPrice(String barcode, BigDecimal newPrice, String updatedBy) {
        validateRequiredText(barcode, "Barcode is required");
        validateMoney(newPrice, "New price must be greater than zero");

        Product product = productRepository.findActiveByBarcode(barcode.trim())
            .orElseThrow(() -> new IllegalArgumentException("Product not found with barcode: " + barcode));

        BigDecimal oldPrice = product.getSellingPrice();
        product.setSellingPrice(newPrice);
        Product saved = productRepository.save(product);
        recordPriceUpdateIfChanged(saved, oldPrice, newPrice, updatedBy);

        Map<String, Object> response = new HashMap<>();
        response.put("barcode", saved.getBarcode());
        response.put("name", saved.getName());
        response.put("oldPrice", oldPrice);
        response.put("newPrice", saved.getSellingPrice());
        response.put("updatedBy", defaultUpdatedBy(updatedBy));
        return response;
    }

    @Transactional
    public Map<String, Object> updateProduct(Long productId,
                                             String barcode,
                                             String name,
                                             String description,
                                             String categoryName,
                                             BigDecimal costPrice,
                                             BigDecimal sellingPrice,
                                             Integer stockQuantity,
                                             String expiryDate,
                                             String updatedBy) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        validateRequiredText(barcode, "Barcode is required");
        validateRequiredText(name, "Product name is required");
        validateRequiredText(categoryName, "Category name is required");
        validateMoney(costPrice, "Cost price must be greater than zero");
        validateMoney(sellingPrice, "Selling price must be greater than zero");

        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity must be zero or greater");
        }

        Product product = findActiveProductById(productId);
        String normalizedBarcode = barcode.trim();

        if (!product.getBarcode().equalsIgnoreCase(normalizedBarcode)) {
            Optional<Product> existingProduct = productRepository.findByBarcode(normalizedBarcode);
            if (existingProduct.isPresent() && !existingProduct.get().getId().equals(productId)) {
                throw new IllegalArgumentException("Product with this barcode already exists");
            }
        }

        Category category = categoryRepository.findByName(categoryName.trim())
            .orElseGet(() -> {
                Category newCategory = new Category();
                newCategory.setName(categoryName.trim());
                newCategory.setDescription("Created by admin dashboard");
                return categoryRepository.save(newCategory);
            });

        BigDecimal oldSellingPrice = product.getSellingPrice();
        product.setBarcode(normalizedBarcode);
        product.setName(name.trim());
        product.setDescription(Optional.ofNullable(description).orElse("").trim());
        product.setCategory(category);
        product.setCostPrice(costPrice);
        product.setSellingPrice(sellingPrice);
        product.setStockQuantity(stockQuantity);
        product.setExpiryDate(parseDate(expiryDate));

        Product saved = productRepository.save(product);
        recordPriceUpdateIfChanged(saved, oldSellingPrice, sellingPrice, updatedBy);

        Map<String, Object> response = toProductResponse(saved);
        response.put("message", "Product updated successfully");
        return response;
    }

    @Transactional
    public Map<String, Object> registerCashier(String username, String password) {
        validateRequiredText(username, "Cashier username is required");
        validateRequiredText(password, "Cashier password is required");

        if (password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must have at least 6 characters");
        }

        String normalizedUsername = username.trim();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User cashier = new User();
        cashier.setUsername(normalizedUsername);
        cashier.setPassword(passwordEncoder.encode(password.trim()));
        cashier.setRole("CASHIER");

        User saved = userRepository.save(cashier);
        return Map.of(
            "id", saved.getId(),
            "username", saved.getUsername(),
            "role", saved.getRole()
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCashiers() {
        return userRepository.findByRole("CASHIER").stream()
            .map(user -> Map.<String, Object>of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProducts() {
        return productRepository.findActiveProducts().stream()
            .map(this::toProductResponse)
            .toList();
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findActiveProductById(productId);
        product.setDeleted(true);
        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentSales(int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        List<Sale> sales = saleRepository.findRecentSales();

        return sales.stream()
            .limit(safeLimit)
            .map(this::toSaleSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSaleDetails(Long saleId) {
        if (saleId == null || saleId <= 0) {
            throw new IllegalArgumentException("Invalid sale ID");
        }
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found"));
        return toSaleDetails(sale);
    }

    @Transactional
    public Map<String, Object> updateSale(Long saleId,
                                          String paymentMethodRaw,
                                          BigDecimal amountReceived,
                                          String transactionReference) {
        if (saleId == null || saleId <= 0) {
            throw new IllegalArgumentException("Invalid sale ID");
        }
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new IllegalArgumentException("Sale not found"));

        PaymentMethod paymentMethod = parsePaymentMethod(paymentMethodRaw);
        if (amountReceived == null || amountReceived.compareTo(sale.getTotalPrice()) < 0) {
            throw new IllegalArgumentException("Amount received must be greater than or equal to total amount");
        }

        String normalizedReference = transactionReference == null ? "" : transactionReference.trim();
        if ((paymentMethod == PaymentMethod.MOBILE_MONEY || paymentMethod == PaymentMethod.CARD)
            && normalizedReference.isEmpty()) {
            throw new IllegalArgumentException("Transaction reference is required for Mobile Money and Card");
        }

        sale.setPaymentMethod(paymentMethod);
        sale.setAmountReceived(amountReceived);
        sale.setChangeAmount(amountReceived.subtract(sale.getTotalPrice()));
        sale.setTransactionReference(normalizedReference.isEmpty() ? null : normalizedReference);

        Sale saved = saleRepository.save(sale);
        Map<String, Object> response = toSaleDetails(saved);
        response.put("message", "Sale updated successfully");
        return response;
    }

    private Product findActiveProductById(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new IllegalArgumentException("Product already deleted");
        }
        return product;
    }

    private Map<String, Object> toProductResponse(Product product) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", product.getId());
        response.put("barcode", product.getBarcode());
        response.put("name", product.getName());
        response.put("description", product.getDescription());
        response.put("category", product.getCategory() == null ? "" : product.getCategory().getName());
        response.put("costPrice", product.getCostPrice());
        response.put("sellingPrice", product.getSellingPrice());
        response.put("stockQuantity", product.getStockQuantity());
        response.put("expiryDate", product.getExpiryDate());
        response.put("isExpired", product.getIsExpired());
        response.put("deleted", product.getDeleted());
        response.put("deletedAt", product.getDeletedAt());
        return response;
    }

    private Map<String, Object> toSaleSummary(Sale sale) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", sale.getId());
        response.put("timestamp", sale.getTimestamp());
        response.put("cashier", sale.getCashier().getUsername());
        response.put("totalPrice", sale.getTotalPrice());
        response.put("profit", calculateSaleProfit(sale));
        response.put("paymentMethod", sale.getPaymentMethod() == null ? "" : sale.getPaymentMethod().name());
        response.put("amountReceived", sale.getAmountReceived());
        response.put("changeAmount", sale.getChangeAmount());
        response.put("transactionReference", sale.getTransactionReference() == null ? "" : sale.getTransactionReference());
        response.put("itemCount", sale.getSaleItems() == null ? 0 : sale.getSaleItems().size());
        return response;
    }

    private Map<String, Object> toSaleDetails(Sale sale) {
        Map<String, Object> response = toSaleSummary(sale);
        List<Map<String, Object>> items = sale.getSaleItems() == null ? List.of() : sale.getSaleItems().stream()
            .map(this::toSaleItemResponse)
            .toList();
        response.put("items", items);
        return response;
    }

    private Map<String, Object> toSaleItemResponse(SaleItem item) {
        Product product = item.getProduct();
        BigDecimal lineTotal = item.getPriceAtSale().multiply(BigDecimal.valueOf(item.getQuantity()));

        Map<String, Object> response = new HashMap<>();
        response.put("id", item.getId());
        response.put("productId", product.getId());
        response.put("barcode", product.getBarcode());
        response.put("productName", product.getName());
        response.put("quantity", item.getQuantity());
        response.put("priceAtSale", item.getPriceAtSale());
        response.put("lineTotal", lineTotal);
        return response;
    }

    private BigDecimal calculateSaleProfit(Sale sale) {
        if (sale.getSaleItems() == null || sale.getSaleItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return sale.getSaleItems().stream()
            .map(item -> {
                BigDecimal priceAtSale = item.getPriceAtSale() == null ? BigDecimal.ZERO : item.getPriceAtSale();
                BigDecimal costPrice = item.getProduct() == null || item.getProduct().getCostPrice() == null
                    ? BigDecimal.ZERO
                    : item.getProduct().getCostPrice();
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                return priceAtSale.subtract(costPrice).multiply(BigDecimal.valueOf(quantity));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recordPriceUpdateIfChanged(Product product,
                                            BigDecimal oldPrice,
                                            BigDecimal newPrice,
                                            String updatedBy) {
        if (oldPrice == null || newPrice == null || oldPrice.compareTo(newPrice) == 0) {
            return;
        }

        PriceUpdateHistory history = new PriceUpdateHistory();
        history.setProduct(product);
        history.setProductName(product.getName());
        history.setProductBarcode(product.getBarcode());
        history.setOldPrice(oldPrice);
        history.setNewPrice(newPrice);
        history.setUpdatedBy(defaultUpdatedBy(updatedBy));
        priceUpdateHistoryRepository.save(history);
    }

    private String defaultUpdatedBy(String updatedBy) {
        if (updatedBy == null || updatedBy.trim().isEmpty()) {
            return "system";
        }
        return updatedBy.trim();
    }

    private PaymentMethod parsePaymentMethod(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        try {
            return PaymentMethod.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid payment method");
        }
    }

    private void validateRequiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateMoney(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Expiry date must be in format yyyy-MM-dd");
        }
    }
}
