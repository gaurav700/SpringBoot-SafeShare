package com.shareApp.Payment.entitites;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "paymentInformation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInformation {
    
    @Id
    private String id;    
    private String userId;
    
    private Instant timestamp;
    
    private long totalStorageUsedInBytes;
    
    private long previousStorageInBytes;
    
    private long changeInStorageBytes;
    
    private String actionType; // "UPLOAD", "DELETE", etc.
    
    private String mediaId; // Reference to the media that caused this change
    
    private String fileName; // For easier tracking
}