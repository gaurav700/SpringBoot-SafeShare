// OTPRepository.java - MongoDB Repository
package com.shareApp.Authentication.repositories;

import com.shareApp.Authentication.entities.OTP;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OTPRepository extends MongoRepository<OTP, String> {

    Optional<OTP> findByEmailAndTypeAndVerifiedFalse(String email, OTP.OTPType type);

    // Find active (non-expired, non-verified) OTP
    @Query("{'email': ?0, 'type': ?1, 'verified': false, 'expiryTime': {$gt: ?2}}")
    Optional<OTP> findActiveOtp(String email, OTP.OTPType type, LocalDateTime currentTime);

    // Delete by email and type
    void deleteByEmailAndType(String email, OTP.OTPType type);

    // Count OTPs created after a certain time for rate limiting
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    // Find expired OTPs (for manual cleanup if needed)
    @Query("{'expiryTime': {$lt: ?0}}")
    List<OTP> findExpiredOtps(LocalDateTime currentTime);

    // Delete expired OTPs (manual cleanup - though TTL should handle this)
    @Query(value = "{'expiryTime': {$lt: ?0}}", delete = true)
    void deleteExpiredOtps(LocalDateTime currentTime);

    // Check if OTP exists and is verified
    @Query("{'email': ?0, 'type': ?1, 'verified': true}")
    Optional<OTP> findVerifiedOtp(String email, OTP.OTPType type);
}