package com.shareApp.Payment.services;

import com.shareApp.Media.repository.MediaRepository;
import com.shareApp.Payment.entitites.PaymentInformation;
import com.shareApp.Payment.entitites.StorageUsage;
import com.shareApp.Payment.repositories.PaymentInformationRepository;
import com.shareApp.Payment.repositories.StorageUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentInformationService {

    private final PaymentInformationRepository paymentInfoRepository;
    private final StorageUsageRepository storageUsageRepository;
    private final MediaRepository mediaRepository;

    // Cost per byte per second (adjust as needed)
    private static final double COST_PER_BYTE_PER_SECOND = 0.000000001; // Very small cost

    public PaymentInformation recordStorageChange(String userId, String mediaId,
                                                  String fileName, long fileSizeBytes,
                                                  String actionType) {
        try {
            // Get current total storage for user
            long currentTotalStorage = calculateTotalUserStorage(userId);

            // Get previous storage amount
            long previousStorage = getCurrentUserStorage(userId);

            // Calculate change based on action type
            long changeInStorage = actionType.equals("UPLOAD") ? fileSizeBytes : -fileSizeBytes;

            PaymentInformation paymentInfo = PaymentInformation.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .timestamp(Instant.now())
                    .totalStorageUsedInBytes(currentTotalStorage)
                    .previousStorageInBytes(previousStorage)
                    .changeInStorageBytes(changeInStorage)
                    .actionType(actionType)
                    .mediaId(mediaId)
                    .fileName(fileName)
                    .build();

            PaymentInformation saved = paymentInfoRepository.save(paymentInfo);
            log.info("Storage change recorded for user {}: {} bytes ({})",
                    userId, changeInStorage, actionType);

            // Start new storage tracking period with updated storage amount
            startStorageTracking(userId, currentTotalStorage);

            return saved;

        } catch (Exception e) {
            log.error("Failed to record storage change for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to record storage information", e);
        }
    }

    public long calculateTotalUserStorage(String userId) {
        return mediaRepository.findByUserId(userId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .mapToLong(media -> media.getSizeInBytes())
                .sum();
    }

    public long getCurrentUserStorage(String userId) {
        return paymentInfoRepository.findFirstByUserIdOrderByTimestampDesc(userId)
                .map(PaymentInformation::getTotalStorageUsedInBytes)
                .orElse(0L);
    }

    public Page<PaymentInformation> getUserStorageHistory(String userId, int page, int size) {
        return paymentInfoRepository.findByUserIdOrderByTimestampDesc(
                userId, PageRequest.of(page, size));
    }

    public PaymentInformation getLatestStorageInfo(String userId) {
        return paymentInfoRepository.findFirstByUserIdOrderByTimestampDesc(userId)
                .orElse(null);
    }

    // Time-based storage tracking methods
    public StorageUsage startStorageTracking(String userId, long storageBytes) {
        // End any active tracking period first
        endActiveStorageTracking(userId);

        StorageUsage usage = StorageUsage.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .timestamp(Instant.now())
                .storageUsedInBytes(storageBytes)
                .periodStart(Instant.now())
                .costPerBytePerSecond(COST_PER_BYTE_PER_SECOND)
                .status("ACTIVE")
                .build();

        StorageUsage saved = storageUsageRepository.save(usage);
        log.info("Started storage tracking for user {}: {} bytes", userId, storageBytes);
        return saved;
    }

    public StorageUsage endActiveStorageTracking(String userId) {
        Optional<StorageUsage> activeUsage = storageUsageRepository
                .findFirstByUserIdAndStatusOrderByTimestampDesc(userId, "ACTIVE");

        if (activeUsage.isPresent()) {
            StorageUsage usage = activeUsage.get();
            Instant now = Instant.now();

            usage.setPeriodEnd(now);
            usage.setDurationInSeconds(now.getEpochSecond() - usage.getPeriodStart().getEpochSecond());
            usage.setCalculatedCost(calculateStorageCost(usage.getStorageUsedInBytes(), usage.getDurationInSeconds()));
            usage.setStatus("COMPLETED");

            StorageUsage saved = storageUsageRepository.save(usage);
            log.info("Ended storage tracking for user {}: {} bytes for {} seconds, cost: ${}",
                    userId, usage.getStorageUsedInBytes(), usage.getDurationInSeconds(),
                    usage.getCalculatedCost());
            return saved;
        }
        return null;
    }

    public double calculateStorageCost(long storageBytes, long durationSeconds) {
        return storageBytes * durationSeconds * COST_PER_BYTE_PER_SECOND;
    }

    public double getTotalCostForPeriod(String userId, Instant start, Instant end) {
        List<StorageUsage> usages = storageUsageRepository.findByUserIdAndPeriodBetween(userId, start, end);
        return usages.stream()
                .mapToDouble(StorageUsage::getCalculatedCost)
                .sum();
    }

    public double getCurrentDailyCost(String userId) {
        Instant dayStart = Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
        return getTotalCostForPeriod(userId, dayStart, Instant.now());
    }

    public Page<StorageUsage> getUserStorageUsageHistory(String userId, int page, int size) {
        return storageUsageRepository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(page, size));
    }

    public Map<String, Object> getMonthlyStorageStats(String userId, Instant monthStart, Instant monthEnd) {
        List<StorageUsage> monthlyUsages = storageUsageRepository.findByUserIdAndTimestampBetween(userId, monthStart, monthEnd);

        double totalCost = 0.0;
        long totalStorageSeconds = 0;
        long maxStorage = 0;
        long minStorage = Long.MAX_VALUE;
        double avgStorage = 0.0;

        if (!monthlyUsages.isEmpty()) {
            totalCost = monthlyUsages.stream()
                    .mapToDouble(StorageUsage::getCalculatedCost)
                    .sum();

            totalStorageSeconds = monthlyUsages.stream()
                    .mapToLong(usage -> usage.getStorageUsedInBytes() * usage.getDurationInSeconds())
                    .sum();

            maxStorage = monthlyUsages.stream()
                    .mapToLong(StorageUsage::getStorageUsedInBytes)
                    .max().orElse(0);

            minStorage = monthlyUsages.stream()
                    .mapToLong(StorageUsage::getStorageUsedInBytes)
                    .min().orElse(0);

            long totalSeconds = monthlyUsages.stream()
                    .mapToLong(StorageUsage::getDurationInSeconds)
                    .sum();

            avgStorage = totalSeconds > 0 ? (double) totalStorageSeconds / totalSeconds : 0.0;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCost", totalCost);
        stats.put("maxStorageBytes", maxStorage);
        stats.put("minStorageBytes", minStorage);
        stats.put("avgStorageBytes", avgStorage);
        stats.put("maxStorageMB", maxStorage / (1024.0 * 1024.0));
        stats.put("minStorageMB", minStorage / (1024.0 * 1024.0));
        stats.put("avgStorageMB", avgStorage / (1024.0 * 1024.0));
        stats.put("totalUsagePeriods", monthlyUsages.size());
        stats.put("costPerMBPerDay", calculateCostPerMBPerDay());

        return stats;
    }

    public double calculateCostPerMBPerDay() {
        long bytesPerMB = 1024 * 1024;
        long secondsPerDay = 24 * 60 * 60;
        return COST_PER_BYTE_PER_SECOND * bytesPerMB * secondsPerDay;
    }

    public double calculateMonthlyStorageCost(String userId, int month, int year) {
        java.time.LocalDate startOfMonth = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

        Instant start = startOfMonth.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant end = endOfMonth.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant();

        return getTotalCostForPeriod(userId, start, end);
    }
}