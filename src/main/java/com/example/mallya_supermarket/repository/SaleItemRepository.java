package com.example.mallya_supermarket.repository;

import com.example.mallya_supermarket.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    @Query("SELECT si.product.id, COALESCE(SUM(si.quantity), 0) FROM SaleItem si GROUP BY si.product.id")
    List<Object[]> sumSoldQuantityByProductId();

    @Query("SELECT si FROM SaleItem si JOIN si.product p WHERE p.isExpired = true")
    List<SaleItem> findExpiredProductsSold();

    @Query("SELECT si FROM SaleItem si WHERE si.product.id = :productId")
    List<SaleItem> findByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(si) FROM SaleItem si JOIN si.product p WHERE p.isExpired = true")
    Long countExpiredProductsSold();

    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM SaleItem si JOIN si.product p WHERE p.isExpired = true")
    Long countTotalExpiredProductsSold();
}
