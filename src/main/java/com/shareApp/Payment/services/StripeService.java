package com.shareApp.Payment.services;

import com.shareApp.Payment.dto.PaymentRequestDTO;
import com.shareApp.Payment.dto.StripeResponseDTO;
import com.shareApp.Payment.entitites.Payment;
import com.shareApp.Payment.repositories.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class StripeService {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    // Option 1: Increase price per GB to meet minimum
    private static final BigDecimal PRICE_PER_GB = new BigDecimal("0.10"); // $0.10 per GB

    // Option 2: Set minimum charge amount
    private static final BigDecimal MINIMUM_CHARGE = new BigDecimal("0.50"); // $0.50 minimum

    private final PaymentRepository paymentRepository;

    public StripeService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public StripeResponseDTO checkoutProducts(String userId, PaymentRequestDTO paymentRequest) {
        try {
            // Calculate base amount
            BigDecimal baseAmount = PRICE_PER_GB.multiply(new BigDecimal(paymentRequest.getStorageGb()));

            // Apply minimum charge if necessary
            BigDecimal totalAmount = baseAmount.max(MINIMUM_CHARGE);

            // Log the calculation for debugging
            log.info("Storage: {}GB, Base amount: ${}, Final amount: ${}",
                    paymentRequest.getStorageGb(), baseAmount, totalAmount);

            // Convert to cents for Stripe (round to avoid precision issues)
            Long amountInCents = totalAmount.multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            log.info("Amount in cents for Stripe: {}", amountInCents);

            // Validate minimum amount (Stripe requires at least 50 cents)
            if (amountInCents < 50) {
                throw new IllegalArgumentException("Amount must be at least $0.50 USD");
            }

            // Set Stripe API key
            Stripe.apiKey = secretKey;

            // Create product data
            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Cloud Storage - " + paymentRequest.getStorageGb() + "GB")
                            .setDescription("Premium cloud storage subscription")
                            .build();

            // Create price data
            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(paymentRequest.getCurrency().toLowerCase())
                            .setUnitAmount(amountInCents)
                            .setProductData(productData)
                            .build();

            // Create line item
            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(priceData)
                            .build();

            // Create session parameters
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(paymentRequest.getSuccessUrl() != null ?
                            paymentRequest.getSuccessUrl() : baseUrl + "/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(paymentRequest.getCancelUrl() != null ?
                            paymentRequest.getCancelUrl() : baseUrl + "/cancel")
                    .addLineItem(lineItem)
                    .putMetadata("userId", userId)
                    .putMetadata("storageGb", paymentRequest.getStorageGb().toString())
                    .build();

            // Create Stripe session
            Session session = Session.create(params);

            // Save payment record to database
            Payment payment = Payment.builder()
                    .userId(userId)
                    .stripeSessionId(session.getId())
                    .storageGb(paymentRequest.getStorageGb())
                    .amount(totalAmount)
                    .currency(paymentRequest.getCurrency())
                    .status(Payment.PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);

            log.info("Payment session created for user: {} with session ID: {}", userId, session.getId());

            return StripeResponseDTO.builder()
                    .status("SUCCESS")
                    .message("Payment session created successfully")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error occurred: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment amount: {}", e.getMessage());
            throw new RuntimeException("Invalid payment amount: " + e.getMessage());
        }
    }

    // Method to handle webhook and update payment status
    public void updatePaymentStatus(String sessionId, Payment.PaymentStatus status) {
        paymentRepository.findByStripeSessionId(sessionId)
                .ifPresent(payment -> {
                    payment.setStatus(status);
                    paymentRepository.save(payment);
                    log.info("Payment status updated for session: {} to: {}", sessionId, status);
                });
    }
}