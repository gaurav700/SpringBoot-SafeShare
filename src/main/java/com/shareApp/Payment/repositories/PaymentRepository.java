package com.shareApp.Payment.repositories;

import com.shareApp.Payment.entitites.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByUserId(String userId);

    Optional<Payment> findByStripeSessionId(String sessionId);

    @Query("{'userId': ?0, 'status': ?1}")
    List<Payment> findByUserIdAndStatus(String userId, Payment.PaymentStatus status);

    @Query(value = "{'userId': ?0}", sort = "{'createdAt': -1}")
    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);
}