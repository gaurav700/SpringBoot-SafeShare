package com.shareApp.Payment.controller;

import com.shareApp.Payment.entitites.PaymentInformation;
import com.shareApp.Payment.entitites.StorageUsage;
import com.shareApp.Payment.services.PaymentInformationService;
import com.shareApp.Utils.security.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment-info")
@RequiredArgsConstructor
public class PaymentInformationController {

    private final PaymentInformationService paymentInformationService;
    private final JWTService jwtService;

    @GetMapping("/storage-history")
    public ResponseEntity<Page<PaymentInformation>> getStorageHistory(
            @RequestHeader("Authorization") String authToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);
        Page<PaymentInformation> history = paymentInformationService.getUserStorageHistory(userId, page, size);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/current-storage")
    public ResponseEntity<Map<String, Object>> getCurrentStorage(
            @RequestHeader("Authorization") String authToken
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);

        long currentStorage = paymentInformationService.calculateTotalUserStorage(userId);
        PaymentInformation latestInfo = paymentInformationService.getLatestStorageInfo(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("currentStorageInBytes", currentStorage);
        response.put("currentStorageInMB", currentStorage / (1024.0 * 1024.0));
        response.put("currentStorageInGB", currentStorage / (1024.0 * 1024.0 * 1024.0));
        response.put("lastUpdated", latestInfo != null ? latestInfo.getTimestamp() : null);
        response.put("totalRecords", latestInfo != null ? "Available" : "No records found");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-bill")
    public ResponseEntity<Map<String, Object>> getMonthlyBill(
            @RequestHeader("Authorization") String authToken,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);

        // Default to current month if not specified
        java.time.LocalDate now = java.time.LocalDate.now();
        int targetMonth = month != null ? month : now.getMonthValue();
        int targetYear = year != null ? year : now.getYear();

        double monthlyCost = paymentInformationService.calculateMonthlyStorageCost(userId, targetMonth, targetYear);
        long currentStorage = paymentInformationService.calculateTotalUserStorage(userId);

        // Calculate daily average cost for the month
        java.time.LocalDate startOfMonth = java.time.LocalDate.of(targetYear, targetMonth, 1);
        int daysInMonth = startOfMonth.lengthOfMonth();
        double dailyAverageCost = monthlyCost / daysInMonth;

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("month", targetMonth);
        response.put("year", targetYear);
        response.put("monthName", startOfMonth.getMonth().toString());
        response.put("totalMonthlyBillUSD", String.format("%.6f", monthlyCost));
        response.put("dailyAverageCostUSD", String.format("%.6f", dailyAverageCost));
        response.put("currentStorageBytes", currentStorage);
        response.put("currentStorageMB", String.format("%.2f", currentStorage / (1024.0 * 1024.0)));
        response.put("currentStorageGB", String.format("%.4f", currentStorage / (1024.0 * 1024.0 * 1024.0)));
        response.put("daysInMonth", daysInMonth);
        response.put("costPerMBPerDay", String.format("%.8f", paymentInformationService.calculateCostPerMBPerDay()));
        response.put("billingPeriod", startOfMonth + " to " + startOfMonth.plusMonths(1).minusDays(1));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<PaymentInformation> getLatestStorageInfo(
            @RequestHeader("Authorization") String authToken
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);
        PaymentInformation latest = paymentInformationService.getLatestStorageInfo(userId);

        if (latest == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(latest);
    }

    @GetMapping("/usage-history")
    public ResponseEntity<Page<StorageUsage>> getStorageUsageHistory(
            @RequestHeader("Authorization") String authToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);
        Page<StorageUsage> usage = paymentInformationService.getUserStorageUsageHistory(userId, page, size);
        return ResponseEntity.ok(usage);
    }

    @GetMapping("/daily-cost")
    public ResponseEntity<Map<String, Object>> getDailyCost(
            @RequestHeader("Authorization") String authToken
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);
        double dailyCost = paymentInformationService.getCurrentDailyCost(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("dailyCostUSD", dailyCost);
        response.put("period", "Last 24 hours");
        response.put("timestamp", java.time.Instant.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cost-calculation")
    public ResponseEntity<Map<String, Object>> getCostCalculation(
            @RequestHeader("Authorization") String authToken,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        String userId = jwtService.validateTokenAndGetUserId(authToken);

        // Default to current month if not specified
        java.time.LocalDate now = java.time.LocalDate.now();
        int targetMonth = month != null ? month : now.getMonthValue();
        int targetYear = year != null ? year : now.getYear();

        // Calculate start and end of the month
        java.time.LocalDate startOfMonth = java.time.LocalDate.of(targetYear, targetMonth, 1);
        java.time.LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        java.time.Instant start = startOfMonth.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant end = endOfMonth.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant();

        double totalCost = paymentInformationService.getTotalCostForPeriod(userId, start, end);
        long currentStorage = paymentInformationService.calculateTotalUserStorage(userId);

        // Calculate monthly statistics
        Map<String, Object> monthlyStats = paymentInformationService.getMonthlyStorageStats(userId, start, end);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("month", targetMonth);
        response.put("year", targetYear);
        response.put("monthName", startOfMonth.getMonth().toString());
        response.put("totalCostUSD", totalCost);
        response.put("periodStart", start);
        response.put("periodEnd", end);
        response.put("currentStorageBytes", currentStorage);
        response.put("currentStorageMB", currentStorage / (1024.0 * 1024.0));
        response.put("currentStorageGB", currentStorage / (1024.0 * 1024.0 * 1024.0));
        response.put("monthlyStats", monthlyStats);

        return ResponseEntity.ok(response);
    }
}