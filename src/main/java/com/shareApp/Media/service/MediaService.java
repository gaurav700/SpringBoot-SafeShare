package com.shareApp.Media.service;

import com.shareApp.Media.model.Media;
import com.shareApp.Media.repository.MediaRepository;
import com.shareApp.Media.storage.MediaStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final MediaStorage mediaStorage;

    public Media upload(String userId, MultipartFile file) {
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

        return mediaRepository.save(media);
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
}
