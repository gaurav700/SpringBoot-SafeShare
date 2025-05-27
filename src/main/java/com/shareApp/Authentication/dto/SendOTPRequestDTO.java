package com.shareApp.Authentication.dto;

import com.shareApp.Authentication.entities.OTP;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOTPRequestDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private OTP.OTPType type = OTP.OTPType.SIGNUP;
}