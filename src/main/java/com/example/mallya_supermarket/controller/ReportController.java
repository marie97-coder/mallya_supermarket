package com.example.mallya_supermarket.controller;

import com.example.mallya_supermarket.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {
    
    private final ReportService reportService;
    
    @GetMapping("/revenue/{period}")
    public ResponseEntity<?> getRevenueReport(@PathVariable String period) {
        try {
            Map<String, Object> report = reportService.getRevenueReport(period);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid period. Use 'daily' or 'monthly'");
        } catch (Exception e) {
            log.error("Error generating revenue report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating report");
        }
    }
    
    @GetMapping("/wastage")
    public ResponseEntity<?> getWastageReport() {
        try {
            Map<String, Object> report = reportService.getWastageReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating wastage report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating report");
        }
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockReport(@RequestParam(defaultValue = "10") int threshold) {
        try {
            Map<String, Object> report = reportService.getLowStockReport(threshold);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating low stock report: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating report");
        }
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardSummary() {
        try {
            Map<String, Object> summary = reportService.getDashboardSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error generating dashboard summary: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error generating dashboard summary");
        }
    }
}
