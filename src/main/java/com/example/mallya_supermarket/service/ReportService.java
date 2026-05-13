package com.example.mallya_supermarket.service;

import com.example.mallya_supermarket.entity.PriceUpdateHistory;
import com.example.mallya_supermarket.entity.Product;
import com.example.mallya_supermarket.entity.Sale;
import com.example.mallya_supermarket.entity.SaleItem;
import com.example.mallya_supermarket.repository.PriceUpdateHistoryRepository;
import com.example.mallya_supermarket.repository.ProductRepository;
import com.example.mallya_supermarket.repository.SaleItemRepository;
import com.example.mallya_supermarket.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final PriceUpdateHistoryRepository priceUpdateHistoryRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getProfitLossReport(String period) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        switch (period.toLowerCase()) {
            case "daily":
                startDate = LocalDateTime.of(endDate.toLocalDate(), LocalTime.MIN);
                break;
            case "monthly":
                startDate = LocalDateTime.of(endDate.toLocalDate().withDayOfMonth(1), LocalTime.MIN);
                break;
            default:
                throw new IllegalArgumentException("Period must be 'daily' or 'monthly'");
        }

        List<Sale> sales = saleRepository.findSalesBetween(startDate, endDate);
        BigDecimal taxCollected = BigDecimal.ZERO;
        BigDecimal salesAmount = BigDecimal.ZERO;
        BigDecimal costOfGoodsSold = BigDecimal.ZERO;

        for (Sale sale : sales) {
            taxCollected = taxCollected.add(sale.getTax() == null ? BigDecimal.ZERO : sale.getTax());

            List<SaleItem> saleItems = saleItemRepository.findBySaleId(sale.getId());
            for (SaleItem item : saleItems) {
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
                BigDecimal itemSales = item.getPriceAtSale().multiply(qty);
                BigDecimal itemCost = item.getProduct().getCostPrice().multiply(qty);

                salesAmount = salesAmount.add(itemSales);
                costOfGoodsSold = costOfGoodsSold.add(itemCost);
            }
        }

        List<Product> expiredProducts = productRepository.findByIsExpiredTrue();
        BigDecimal wastageLoss = expiredProducts.stream()
            .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossProfit = salesAmount.subtract(costOfGoodsSold);
        BigDecimal netProfit = grossProfit.subtract(wastageLoss);

        Map<String, Object> report = new HashMap<>();
        report.put("period", period.toLowerCase());
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalSalesAmount", salesAmount);
        report.put("taxCollected", taxCollected);
        report.put("costOfGoodsSold", costOfGoodsSold);
        report.put("grossProfit", grossProfit);
        report.put("wastageLoss", wastageLoss);
        report.put("netProfit", netProfit);
        report.put("saleCount", sales.size());
        report.put("expiredProductsCount", expiredProducts.size());

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProductActivityReport(String query) {
        String keyword = query == null ? "" : query.trim().toLowerCase();

        Map<Long, Long> soldByProduct = saleItemRepository.sumSoldQuantityByProductId().stream()
            .collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
            ));

        List<Product> activeProducts = productRepository.findActiveProducts();
        List<Product> deletedProducts = productRepository.findDeletedProducts();

        if (!keyword.isBlank()) {
            activeProducts = activeProducts.stream()
                .filter(product -> containsKeyword(product, keyword))
                .toList();
            deletedProducts = deletedProducts.stream()
                .filter(product -> containsKeyword(product, keyword))
                .toList();
        }

        List<Map<String, Object>> currentProducts = activeProducts.stream()
            .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
            .map(product -> toProductActivityRow(product, soldByProduct))
            .toList();

        List<Map<String, Object>> deletedProductRows = deletedProducts.stream()
            .sorted((a, b) -> {
                LocalDateTime first = a.getDeletedAt();
                LocalDateTime second = b.getDeletedAt();
                if (first == null && second == null) return 0;
                if (first == null) return 1;
                if (second == null) return -1;
                return second.compareTo(first);
            })
            .map(product -> toDeletedProductRow(product, soldByProduct))
            .toList();

        List<PriceUpdateHistory> history = keyword.isBlank()
            ? priceUpdateHistoryRepository.findTop100ByOrderByUpdatedAtDesc()
            : priceUpdateHistoryRepository
                .findTop100ByProductNameContainingIgnoreCaseOrProductBarcodeContainingIgnoreCaseOrderByUpdatedAtDesc(
                    keyword,
                    keyword
                );

        List<Map<String, Object>> priceUpdates = new ArrayList<>();
        for (PriceUpdateHistory item : history) {
            Map<String, Object> row = new HashMap<>();
            row.put("productName", item.getProductName());
            row.put("productBarcode", item.getProductBarcode());
            row.put("oldPrice", item.getOldPrice());
            row.put("newPrice", item.getNewPrice());
            row.put("updatedBy", item.getUpdatedBy());
            row.put("updatedAt", item.getUpdatedAt());
            priceUpdates.add(row);
        }

        Map<String, Object> daily = getProfitLossReport("daily");
        Map<String, Object> monthly = getProfitLossReport("monthly");

        Map<String, Object> report = new HashMap<>();
        report.put("generatedAt", LocalDateTime.now());
        report.put("query", query == null ? "" : query.trim());
        report.put("currentProducts", currentProducts);
        report.put("deletedProducts", deletedProductRows);
        report.put("priceUpdates", priceUpdates);
        report.put("profitDaily", toProfitSummary(daily));
        report.put("profitMonthly", toProfitSummary(monthly));
        report.put("activeProductsCount", currentProducts.size());
        report.put("deletedProductsCount", deletedProductRows.size());
        report.put("priceUpdateCount", priceUpdates.size());

        return report;
    }
    
    public Map<String, Object> getRevenueReport(String period) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();
        
        switch (period.toLowerCase()) {
            case "daily":
                startDate = LocalDateTime.of(endDate.toLocalDate(), LocalTime.MIN);
                break;
            case "monthly":
                startDate = LocalDateTime.of(endDate.toLocalDate().withDayOfMonth(1), LocalTime.MIN);
                break;
            default:
                throw new IllegalArgumentException("Period must be 'daily' or 'monthly'");
        }
        
        BigDecimal revenue = saleRepository.calculateRevenueBetween(startDate, endDate);
        Long saleCount = saleRepository.countSalesBetween(startDate, endDate);
        List<Sale> sales = saleRepository.findSalesBetween(startDate, endDate);
        
        Map<String, Object> report = new HashMap<>();
        report.put("period", period);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("totalRevenue", revenue);
        report.put("totalSales", saleCount);
        report.put("averageSaleValue", saleCount > 0 ? revenue.divide(BigDecimal.valueOf(saleCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        report.put("sales", sales);
        
        return report;
    }
    
    public Map<String, Object> getWastageReport() {
        List<Product> expiredProducts = productRepository.findByIsExpiredTrue();
        List<SaleItem> expiredProductsSold = saleItemRepository.findExpiredProductsSold();
        
        BigDecimal totalWastage = BigDecimal.ZERO;
        for (Product product : expiredProducts) {
            BigDecimal loss = product.getCostPrice().multiply(BigDecimal.valueOf(product.getStockQuantity()));
            totalWastage = totalWastage.add(loss);
        }
        
        BigDecimal revenueFromExpired = BigDecimal.ZERO;
        for (SaleItem item : expiredProductsSold) {
            BigDecimal revenue = item.getPriceAtSale().multiply(BigDecimal.valueOf(item.getQuantity()));
            revenueFromExpired = revenueFromExpired.add(revenue);
        }
        
        Map<String, Object> report = new HashMap<>();
        report.put("expiredProducts", expiredProducts);
        report.put("expiredProductsSold", expiredProductsSold);
        report.put("totalWastageValue", totalWastage);
        report.put("revenueFromExpiredProducts", revenueFromExpired);
        report.put("netLoss", totalWastage.subtract(revenueFromExpired));
        report.put("expiredProductCount", expiredProducts.size());
        
        return report;
    }
    
    public Map<String, Object> getLowStockReport(int threshold) {
        List<Product> lowStockProducts = productRepository.findLowStockProducts(threshold);
        
        Map<String, Object> report = new HashMap<>();
        report.put("lowStockProducts", lowStockProducts);
        report.put("threshold", threshold);
        report.put("lowStockCount", lowStockProducts.size());
        
        return report;
    }
    
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> dailyReport = getRevenueReport("daily");
        Map<String, Object> monthlyReport = getRevenueReport("monthly");
        Map<String, Object> wastageReport = getWastageReport();
        Map<String, Object> lowStockReport = getLowStockReport(10);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("dailyRevenue", dailyReport.get("totalRevenue"));
        summary.put("dailySales", dailyReport.get("totalSales"));
        summary.put("monthlyRevenue", monthlyReport.get("totalRevenue"));
        summary.put("monthlySales", monthlyReport.get("totalSales"));
        summary.put("totalWastage", wastageReport.get("totalWastageValue"));
        summary.put("lowStockCount", lowStockReport.get("lowStockCount"));
        summary.put("expiredProductCount", wastageReport.get("expiredProductCount"));
        
        return summary;
    }

    private boolean containsKeyword(Product product, String keyword) {
        String name = product.getName() == null ? "" : product.getName().toLowerCase();
        String barcode = product.getBarcode() == null ? "" : product.getBarcode().toLowerCase();
        return name.contains(keyword) || barcode.contains(keyword);
    }

    private Map<String, Object> toProductActivityRow(Product product, Map<Long, Long> soldByProduct) {
        long sold = soldByProduct.getOrDefault(product.getId(), 0L);
        int remainingStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        long totalRegistered = remainingStock + sold;

        Map<String, Object> row = new HashMap<>();
        row.put("id", product.getId());
        row.put("barcode", product.getBarcode());
        row.put("name", product.getName());
        row.put("price", product.getSellingPrice());
        row.put("costPrice", product.getCostPrice());
        row.put("totalRegistered", totalRegistered);
        row.put("totalSold", sold);
        row.put("remainingStock", remainingStock);
        row.put("expiryDate", product.getExpiryDate());
        row.put("isExpired", product.getIsExpired());
        return row;
    }

    private Map<String, Object> toDeletedProductRow(Product product, Map<Long, Long> soldByProduct) {
        long sold = soldByProduct.getOrDefault(product.getId(), 0L);
        int remainingStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        Map<String, Object> row = new HashMap<>();
        row.put("id", product.getId());
        row.put("barcode", product.getBarcode());
        row.put("name", product.getName());
        row.put("price", product.getSellingPrice());
        row.put("remainingStock", remainingStock);
        row.put("totalSold", sold);
        row.put("deletedAt", product.getDeletedAt());
        return row;
    }

    private Map<String, Object> toProfitSummary(Map<String, Object> reportData) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("period", reportData.get("period"));
        summary.put("netProfit", nonNegativeMoney(reportData.get("netProfit")));
        summary.put("grossProfit", nonNegativeMoney(reportData.get("grossProfit")));
        summary.put("totalSalesAmount", nonNullMoney(reportData.get("totalSalesAmount")));
        summary.put("costOfGoodsSold", nonNullMoney(reportData.get("costOfGoodsSold")));
        summary.put("wastageLoss", nonNullMoney(reportData.get("wastageLoss")));
        summary.put("taxCollected", nonNullMoney(reportData.get("taxCollected")));
        summary.put("saleCount", reportData.getOrDefault("saleCount", 0));
        summary.put("startDate", reportData.get("startDate"));
        summary.put("endDate", reportData.get("endDate"));
        return summary;
    }

    private BigDecimal nonNullMoney(Object value) {
        if (value instanceof BigDecimal money) {
            return money;
        }
        if (Objects.isNull(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal nonNegativeMoney(Object value) {
        BigDecimal amount = nonNullMoney(value);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }
}
