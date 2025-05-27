package com.shareApp.Payment.repositories;

import com.shareApp.Payment.entitites.StorageUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StorageUsageRepository extends MongoRepository<StorageUsage, String> {
    
    Optional<StorageUsage> findFirstByUserIdAndStatusOrderByTimestampDesc(String userId, String status);
    
    List<StorageUsage> findByUserIdAndTimestampBetween(String userId, Instant start, Instant end);
    
    Page<StorageUsage> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);
    
    @Query("{'userId': ?0, 'status': 'ACTIVE'}")
    List<StorageUsage> findActiveStorageByUserId(String userId);
    
    @Query("{'userId': ?0, 'periodStart': {$gte: ?1}, 'periodEnd': {$lte: ?2}}")
    List<StorageUsage> findByUserIdAndPeriodBetween(String userId, Instant start, Instant end);
}