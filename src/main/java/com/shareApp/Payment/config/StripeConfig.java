package com.shareApp.Payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StripeConfig {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @PostConstruct
    public void setup() {
        try {
            if (secretKey == null || secretKey.trim().isEmpty()) {
                throw new IllegalArgumentException("Stripe secret key is not configured");
            }

            Stripe.apiKey = secretKey;

            // Log configuration status (without exposing sensitive data)
            log.info("Stripe configuration initialized successfully");
            log.info("Secret key configured: {}", secretKey.startsWith("sk_") ? "Yes" : "No");
            log.info("Webhook secret configured: {}",
                    (webhookSecret != null && !webhookSecret.trim().isEmpty()) ? "Yes" : "No");

            // Validate key format
            if (!secretKey.startsWith("sk_")) {
                log.warn("Stripe secret key format may be incorrect. Expected to start with 'sk_'");
            }

        } catch (Exception e) {
            log.error("Failed to initialize Stripe configuration: {}", e.getMessage());
            throw new RuntimeException("Stripe configuration failed", e);
        }
    }

    public boolean isWebhookConfigured() {
        return webhookSecret != null && !webhookSecret.trim().isEmpty();
    }
}