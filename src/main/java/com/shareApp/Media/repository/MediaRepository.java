package com.shareApp.Media.repository;

import com.shareApp.Media.model.Media;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaRepository extends MongoRepository<Media, String> {
    Page<Media> findByUserId(String userId, Pageable pageable);
}
