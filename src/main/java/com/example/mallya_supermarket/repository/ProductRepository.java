package com.example.mallya_supermarket.repository;

import com.example.mallya_supermarket.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    @Query("SELECT p FROM Product p WHERE p.barcode = :barcode AND COALESCE(p.deleted, false) = false")
    Optional<Product> findActiveByBarcode(@Param("barcode") String barcode);

    boolean existsByBarcode(String barcode);

    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false ORDER BY p.name ASC")
    List<Product> findActiveProducts();

    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = true ORDER BY p.deletedAt DESC")
    List<Product> findDeletedProducts();

    @Query("SELECT p FROM Product p WHERE p.isExpired = true AND COALESCE(p.deleted, false) = false")
    List<Product> findByIsExpiredTrue();

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND COALESCE(p.deleted, false) = false")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND COALESCE(p.deleted, false) = false")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("""
        SELECT p FROM Product p
        WHERE COALESCE(p.deleted, false) = false
        AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        ORDER BY p.name ASC
        """)
    List<Product> findTop10ActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.expiryDate BETWEEN :startDate AND :endDate AND COALESCE(p.deleted, false) = false")
    List<Product> findProductsExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity < :threshold AND COALESCE(p.deleted, false) = false ORDER BY p.stockQuantity ASC")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    @Query("SELECT p FROM Product p WHERE p.expiryDate <= :thirtyDaysFromNow AND p.expiryDate > :today AND p.isExpired = false AND COALESCE(p.deleted, false) = false ORDER BY p.expiryDate ASC")
    List<Product> findProductsExpiringSoon(@Param("today") LocalDate today, @Param("thirtyDaysFromNow") LocalDate thirtyDaysFromNow);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isExpired = true AND COALESCE(p.deleted, false) = false")
    Long countExpiredProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity < :threshold AND COALESCE(p.deleted, false) = false")
    Long countLowStockProducts(@Param("threshold") int threshold);
}
