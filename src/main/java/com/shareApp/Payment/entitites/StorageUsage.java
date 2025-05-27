package com.shareApp.Payment.entitites;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "storageUsage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsage {
    
    @Id
    private String id;
    
    private String userId;
    
    private Instant timestamp;
    
    private long storageUsedInBytes;
    
    private Instant periodStart;
    
    private Instant periodEnd;
    
    private long durationInSeconds;
    
    private double costPerBytePerSecond;
    
    private double calculatedCost;
    
    private String status; // "ACTIVE", "COMPLETED", "CALCULATED"
}