package com.shareApp.Payment.repositories;

import com.shareApp.Payment.entitites.PaymentInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface PaymentInformationRepository extends MongoRepository<PaymentInformation, String> {

    Page<PaymentInformation> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Optional<PaymentInformation> findFirstByUserIdOrderByTimestampDesc(String userId);

    @Query(value = "{'userId': ?0}", fields = "{'totalStorageUsedInBytes': 1}", sort = "{'timestamp': -1}")
    Optional<PaymentInformation> findLatestStorageByUserId(String userId);
}