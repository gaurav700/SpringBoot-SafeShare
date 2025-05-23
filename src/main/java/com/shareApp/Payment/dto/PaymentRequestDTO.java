package com.shareApp.Payment.dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private Long storageGb = 5L; // Default 5GB
    private String currency = "USD"; // Default currency
    private String successUrl;
    private String cancelUrl;
}