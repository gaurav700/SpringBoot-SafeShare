package com.shareApp.Media.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorage {
    String upload(String userId, MultipartFile file);
    Resource download(String storageKey);
}
