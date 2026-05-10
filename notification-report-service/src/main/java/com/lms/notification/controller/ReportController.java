package com.lms.notification.controller;

import com.lms.notification.dto.ApiResponse;
import com.lms.notification.dto.DashboardSummaryDTO;
import com.lms.notification.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Reports", description = "Admin endpoints for aggregated reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard summary retrieved", reportService.getAdminDashboardSummary()));
    }

    @GetMapping("/most-borrowed")
    @Operation(summary = "Get most borrowed books")
    public ResponseEntity<ApiResponse<Object>> getMostBorrowedBooks() {
        return ResponseEntity.ok(ApiResponse.ok("Most borrowed retrieved", reportService.getMostBorrowedBooks()));
    }

    @GetMapping("/overdue-summary")
    @Operation(summary = "Get overdue summary")
    public ResponseEntity<ApiResponse<Object>> getOverdueSummary() {
        return ResponseEntity.ok(ApiResponse.ok("Overdue summary retrieved", reportService.getOverdueSummary()));
    }

    @GetMapping("/monthly-stats")
    @Operation(summary = "Get monthly borrowing stats")
    public ResponseEntity<ApiResponse<Object>> getMonthlyStats(@RequestParam(required = false) Integer month,
                                                               @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok("Monthly stats retrieved", reportService.getMonthlyBorrowStats(month, year)));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get monthly revenue")
    public ResponseEntity<ApiResponse<Object>> getRevenue(@RequestParam(required = false) Integer month,
                                                          @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok("Revenue retrieved", reportService.getMonthlyRevenue(month, year)));
    }

    @GetMapping("/category-availability")
    @Operation(summary = "Get category availability")
    public ResponseEntity<ApiResponse<Object>> getCategoryAvailability() {
        return ResponseEntity.ok(ApiResponse.ok("Category availability retrieved", reportService.getCategoryAvailability()));
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Clear report caches")
    @CacheEvict(value = "reports", allEntries = true)
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        return ResponseEntity.ok(ApiResponse.ok("Caches cleared successfully", null));
    }
}
