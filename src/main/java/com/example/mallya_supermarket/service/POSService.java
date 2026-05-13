package com.example.mallya_supermarket.service;

import com.example.mallya_supermarket.dto.SaleItemRequest;
import com.example.mallya_supermarket.entity.*;
import com.example.mallya_supermarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class POSService {
    
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final UserRepository userRepository;
    
    public Product scanProduct(String barcode) {
        log.info("Scanning product with barcode: {}", barcode);

        Product product = productRepository.findActiveByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Product not found with barcode: " + barcode));

        if (product.getIsExpired()) {
            throw new RuntimeException("Product is expired and cannot be sold: " + product.getName());
        }

        if (product.getStockQuantity() <= 0) {
            throw new RuntimeException("Product is out of stock: " + product.getName());
        }

        return product;
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String keyword = query.trim();
        return productRepository
            .findTop10ActiveByKeyword(keyword, PageRequest.of(0, 10))
            .stream()
            .filter(product -> !Boolean.TRUE.equals(product.getIsExpired()))
            .filter(product -> product.getStockQuantity() != null && product.getStockQuantity() > 0)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateSaleTotal(List<SaleItemRequest> saleRequests) {
        if (saleRequests == null || saleRequests.isEmpty()) {
            throw new RuntimeException("Sale items are required");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (SaleItemRequest request : saleRequests) {
            validateSaleRequestItem(request);

            Product product = scanProduct(request.getBarcode());
            if (product.getStockQuantity() < request.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                    ". Available: " + product.getStockQuantity() + ", Requested: " + request.getQuantity());
            }

            BigDecimal itemTotal = product.getSellingPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);
        }

        return totalPrice;
    }
    
    @Transactional
    public Sale completeSale(List<SaleItemRequest> saleRequests,
                             String cashierUsername,
                             PaymentMethod paymentMethod,
                             BigDecimal amountReceived,
                             String transactionReference) {
        log.info("Processing sale for {} items by cashier: {}", saleRequests.size(), cashierUsername);
        
        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new RuntimeException("Cashier not found: " + cashierUsername));
        
        Sale sale = new Sale();
        sale.setCashier(cashier);
        sale.setTimestamp(LocalDateTime.now());
        sale.setSaleItems(new ArrayList<>());
        
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        for (SaleItemRequest request : saleRequests) {
            validateSaleRequestItem(request);
            Product product = scanProduct(request.getBarcode());
            
            if (product.getStockQuantity() < request.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() + 
                        ". Available: " + product.getStockQuantity() + ", Requested: " + request.getQuantity());
            }
            
            product.setStockQuantity(product.getStockQuantity() - request.getQuantity());
            productRepository.save(product);
            
            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setQuantity(request.getQuantity());
            saleItem.setPriceAtSale(product.getSellingPrice());
            
            sale.getSaleItems().add(saleItem);
            
            BigDecimal itemTotal = product.getSellingPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);
        }
        
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal finalTotal = totalPrice;
        sale.setTotalPrice(finalTotal);
        sale.setTax(tax);

        if (paymentMethod == null) {
            throw new RuntimeException("Payment method is required");
        }

        if (amountReceived == null || amountReceived.compareTo(finalTotal) < 0) {
            throw new RuntimeException("Amount received must be greater than or equal to total amount");
        }

        if ((paymentMethod == PaymentMethod.MOBILE_MONEY || paymentMethod == PaymentMethod.CARD)
            && (transactionReference == null || transactionReference.trim().isEmpty())) {
            throw new RuntimeException("Transaction reference is required for Mobile Money and Card payments");
        }

        BigDecimal changeAmount = amountReceived.subtract(finalTotal);

        sale.setPaymentMethod(paymentMethod);
        sale.setAmountReceived(amountReceived);
        sale.setChangeAmount(changeAmount);
        sale.setTransactionReference(
            transactionReference == null || transactionReference.trim().isEmpty()
                ? null
                : transactionReference.trim()
        );
        
        return saleRepository.save(sale);
    }

    private void validateSaleRequestItem(SaleItemRequest request) {
        if (request == null) {
            throw new RuntimeException("Sale item is invalid");
        }
        if (request.getBarcode() == null || request.getBarcode().trim().isEmpty()) {
            throw new RuntimeException("Sale item barcode is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Sale item quantity must be greater than zero");
        }
    }
}
