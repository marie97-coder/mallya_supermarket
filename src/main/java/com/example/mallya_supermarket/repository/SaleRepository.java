package com.example.mallya_supermarket.repository;

import com.example.mallya_supermarket.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    
    @Query("SELECT COALESCE(SUM(s.totalPrice), 0) FROM Sale s WHERE s.timestamp BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE s.timestamp BETWEEN :startDate AND :endDate ORDER BY s.timestamp DESC")
    List<Sale> findSalesBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.timestamp BETWEEN :startDate AND :endDate")
    Long countSalesBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM Sale s WHERE s.cashier.id = :cashierId ORDER BY s.timestamp DESC")
    List<Sale> findByCashierId(@Param("cashierId") Long cashierId);
    
    @Query("SELECT COUNT(s) FROM Sale s WHERE DATE(s.timestamp) = CURRENT_DATE")
    Long countTodaySales();
    
    @Query("SELECT COALESCE(SUM(s.totalPrice), 0) FROM Sale s WHERE DATE(s.timestamp) = CURRENT_DATE")
    BigDecimal calculateTodayRevenue();
    
    @Query("SELECT s FROM Sale s ORDER BY s.timestamp DESC")
    List<Sale> findRecentSales();
}
