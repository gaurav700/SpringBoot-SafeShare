package com.shareApp.Authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OTPResponseDTO {
    private boolean success;
    private String message;
    private Object data;

    public static OTPResponseDTO success(String message) {
        return new OTPResponseDTO(true, message, null);
    }

    public static OTPResponseDTO success(String message, Object data) {
        return new OTPResponseDTO(true, message, data);
    }

    public static OTPResponseDTO error(String message) {
        return new OTPResponseDTO(false, message, null);
    }
}