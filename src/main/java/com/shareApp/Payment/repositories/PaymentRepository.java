package com.shareApp.Payment.repositories;

import com.shareApp.Payment.entitites.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Payment> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Payment> findByUserIdAndStatus(String userId, Payment.PaymentStatus status);

    Long countByUserIdAndStatus(String userId, Payment.PaymentStatus status);
}