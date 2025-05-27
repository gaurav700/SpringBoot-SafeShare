package com.shareApp.Media.service;

import com.shareApp.Media.model.Media;
import com.shareApp.Media.repository.MediaRepository;
import com.shareApp.Media.storage.MediaStorage;
import com.shareApp.Payment.services.PaymentInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final MediaStorage mediaStorage;
    private final PaymentInformationService paymentInformationService;

    @Transactional
    public Media upload(String userId, MultipartFile file) {
        try {
            String mediaId = UUID.randomUUID().toString();
            String contentType = file.getContentType();
            String mediaType = contentType != null && contentType.startsWith("video") ? "VIDEO" : "PHOTO";

            String storageKey = mediaStorage.upload(userId, file);

            Media media = Media.builder()
                    .id(mediaId)
                    .userId(userId)
                    .fileName(file.getOriginalFilename())
                    .fileType(contentType)
                    .mediaType(mediaType)
                    .sizeInBytes(file.getSize())
                    .uploadedAt(Instant.now())
                    .s3Key(storageKey)
                    .build();

            Media savedMedia = mediaRepository.save(media);

            // Record storage change in payment information
            paymentInformationService.recordStorageChange(
                    userId,
                    mediaId,
                    file.getOriginalFilename(),
                    file.getSize(),
                    "UPLOAD"
            );

            log.info("Media uploaded successfully for user {}: {} ({} bytes)",
                    userId, file.getOriginalFilename(), file.getSize());

            return savedMedia;

        } catch (Exception e) {
            log.error("Failed to upload media for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to upload media", e);
        }
    }

    public Page<Media> listUserMedia(String userId, int page, int size) {
        return mediaRepository.findByUserId(userId, PageRequest.of(page, size));
    }

    public Resource download(String userId, String mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Media not found or access denied"));
        return mediaStorage.download(media.getS3Key());
    }

    @Transactional
    public void deleteMedia(String userId, String mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .filter(m -> m.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Media not found or access denied"));

        try {
            // Delete from storage first
            // Note: You'll need to implement delete method in MediaStorage interface
            // mediaStorage.delete(media.getS3Key());

            // Delete from database
            mediaRepository.delete(media);

            // Record storage change in payment information
            paymentInformationService.recordStorageChange(
                    userId,
                    mediaId,
                    media.getFileName(),
                    media.getSizeInBytes(),
                    "DELETE"
            );

            log.info("Media deleted successfully for user {}: {} ({} bytes)",
                    userId, media.getFileName(), media.getSizeInBytes());

        } catch (Exception e) {
            log.error("Failed to delete media for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to delete media", e);
        }
    }
}