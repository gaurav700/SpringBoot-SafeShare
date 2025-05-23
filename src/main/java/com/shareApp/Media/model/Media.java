package com.shareApp.Media.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {
    @Id
    private String id;

    private String userId;
    private String fileName;
    private String fileType; // image/png, video/mp4, etc.
    private String mediaType; // PHOTO or VIDEO
    private long sizeInBytes;
    private Instant uploadedAt;
    private String s3Key;
}
