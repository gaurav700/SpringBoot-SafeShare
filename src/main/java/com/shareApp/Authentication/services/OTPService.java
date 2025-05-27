// OTPService.java - MongoDB Implementation
package com.shareApp.Authentication.services;

import com.shareApp.Authentication.dto.OTPResponseDTO;
import com.shareApp.Authentication.entities.OTP;
import com.shareApp.Authentication.repositories.OTPRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final OTPRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    public OTPResponseDTO sendOTP(String email, OTP.OTPType type) {
        try {
            String normalizedEmail = email.toLowerCase().trim();

            // Check rate limiting (max 3 OTPs per hour)
            if (isRateLimited(normalizedEmail)) {
                return OTPResponseDTO.error("Too many OTP requests. Please try again later.");
            }

            // Clean up any existing OTP for this email and type
            otpRepository.deleteByEmailAndType(normalizedEmail, type);

            // Generate new OTP
            String otpCode = generateOTP();

            // Create OTP document
            OTP otp = new OTP();
            otp.setEmail(normalizedEmail);
            otp.setOtp(otpCode);
            otp.setType(type);
            otp.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));
            otp.setAttempts(0);
            otp.setVerified(false);
            otp.setCreatedAt(LocalDateTime.now());

            // Save OTP to MongoDB
            otpRepository.save(otp);

            // Send email asynchronously
            emailService.sendOTPEmail(normalizedEmail, otpCode, type);

            log.info("OTP sent successfully to email: {} for type: {}", normalizedEmail, type);

            return OTPResponseDTO.success("Verification code sent to your email");

        } catch (Exception e) {
            log.error("Failed to send OTP to email: {}", email, e);
            return OTPResponseDTO.error("Failed to send verification code. Please try again.");
        }
    }

    public OTPResponseDTO verifyOTP(String email, String otpCode) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            String normalizedOtp = otpCode.trim();

            // Find active OTP
            Optional<OTP> otpOptional = otpRepository.findActiveOtp(
                    normalizedEmail, OTP.OTPType.SIGNUP, LocalDateTime.now());

            if (otpOptional.isEmpty()) {
                return OTPResponseDTO.error("Invalid or expired verification code");
            }

            OTP otp = otpOptional.get();

            // Check if expired (double check even though we use TTL)
            if (LocalDateTime.now().isAfter(otp.getExpiryTime())) {
                otpRepository.delete(otp);
                return OTPResponseDTO.error("Verification code has expired");
            }

            // Check max attempts
            if (otp.getAttempts() >= maxAttempts) {
                otpRepository.delete(otp);
                return OTPResponseDTO.error("Maximum verification attempts exceeded. Please request a new code.");
            }

            // Increment attempts
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);

            // Verify OTP
            if (!otp.getOtp().equals(normalizedOtp)) {
                int remainingAttempts = maxAttempts - otp.getAttempts();
                if (remainingAttempts > 0) {
                    return OTPResponseDTO.error(
                            String.format("Invalid verification code. %d attempts remaining.", remainingAttempts));
                } else {
                    otpRepository.delete(otp);
                    return OTPResponseDTO.error("Invalid verification code. Maximum attempts exceeded.");
                }
            }

            // Mark as verified
            otp.setVerified(true);
            otpRepository.save(otp);

            log.info("OTP verified successfully for email: {}", normalizedEmail);

            return OTPResponseDTO.success("Verification code verified successfully");

        } catch (Exception e) {
            log.error("Failed to verify OTP for email: {}", email, e);
            return OTPResponseDTO.error("Failed to verify code. Please try again.");
        }
    }

    private String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    private boolean isRateLimited(String email) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentOtpCount = otpRepository.countByEmailAndCreatedAtAfter(email, oneHourAgo);
        return recentOtpCount >= 3; // Max 3 OTPs per hour
    }

    public boolean isOTPVerified(String email, OTP.OTPType type) {
        String normalizedEmail = email.toLowerCase().trim();
        Optional<OTP> verifiedOtp = otpRepository.findVerifiedOtp(normalizedEmail, type);
        return verifiedOtp.isPresent();
    }

    // Cleanup expired OTPs every hour (backup to TTL)
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredOTPs() {
        try {
            otpRepository.deleteExpiredOtps(LocalDateTime.now());
            log.info("Cleaned up expired OTPs");
        } catch (Exception e) {
            log.error("Failed to cleanup expired OTPs", e);
        }
    }

    // Clean up verified OTPs after signup completion
    public void cleanupVerifiedOTP(String email, OTP.OTPType type) {
        try {
            otpRepository.deleteByEmailAndType(email.toLowerCase().trim(), type);
            log.info("Cleaned up verified OTP for email: {} and type: {}", email, type);
        } catch (Exception e) {
            log.error("Failed to cleanup verified OTP for email: {}", email, e);
        }
    }
}