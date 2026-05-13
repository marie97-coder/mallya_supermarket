package com.example.mallya_supermarket.repository;

import com.example.mallya_supermarket.entity.PriceUpdateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceUpdateHistoryRepository extends JpaRepository<PriceUpdateHistory, Long> {

    List<PriceUpdateHistory> findTop100ByOrderByUpdatedAtDesc();

    List<PriceUpdateHistory> findTop100ByProductNameContainingIgnoreCaseOrProductBarcodeContainingIgnoreCaseOrderByUpdatedAtDesc(
        String productName,
        String productBarcode
    );
}
