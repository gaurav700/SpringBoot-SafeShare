package com.shareApp.Authentication.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document(collection = "otps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(def = "{'email': 1, 'type': 1}")
public class OTP {
    @Id
    private String id;

    @Indexed
    private String email;

    private String otp;

    @Indexed(expireAfterSeconds = 600) // TTL index - expires after 10 minutes
    private LocalDateTime expiryTime;

    private Integer attempts = 0;

    private Boolean verified = false;

    private OTPType type;

    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum OTPType {
        SIGNUP,
        PASSWORD_RESET,
        EMAIL_VERIFICATION
    }
}