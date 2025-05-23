package com.shareApp.Media.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
@Slf4j
public class LocalMediaStorage implements MediaStorage {

    @Value("${media.local.path}")
    private String rootPath;

    @Override
    public String upload(String userId, MultipartFile file) {
        try {
            String originalName = StringUtils.cleanPath(file.getOriginalFilename());
            String uniqueName = UUID.randomUUID() + "-" + originalName;
            Path userDir = Paths.get(rootPath, userId); // rootPath is "uploads"
            Files.createDirectories(userDir);

            Path filePath = userDir.resolve(uniqueName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return userId + "/" + uniqueName; // storage key
        } catch (IOException e) {
            System.out.println("❌ Failed to store file" + e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public Resource download(String storageKey) {
        Path fullPath = Paths.get(rootPath).resolve(storageKey);
        return new FileSystemResource(fullPath);
    }
}
