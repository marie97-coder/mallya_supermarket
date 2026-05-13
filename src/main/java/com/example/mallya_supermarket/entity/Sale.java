package com.example.mallya_supermarket.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount_received", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "change_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;
    
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> saleItems;
    
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (amountReceived == null) {
            amountReceived = BigDecimal.ZERO;
        }
        if (changeAmount == null) {
            changeAmount = BigDecimal.ZERO;
        }
    }
}
