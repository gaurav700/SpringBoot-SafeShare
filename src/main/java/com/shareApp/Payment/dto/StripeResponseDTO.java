package com.shareApp.Payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeResponseDTO {
    private String status;
    private String message;
    private String sessionId;
    private String sessionUrl;
}