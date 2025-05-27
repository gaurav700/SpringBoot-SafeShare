package com.shareApp.Payment.services;

import com.shareApp.Payment.dto.StripeResponseDTO;
import com.shareApp.Payment.entitites.Payment;
import com.shareApp.Payment.repositories.PaymentRepository;
import com.shareApp.Payment.services.PaymentInformationService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    // Minimum charge amount required by Stripe
    private static final BigDecimal MINIMUM_CHARGE = new BigDecimal("0.50"); // $0.50 minimum

    private final PaymentRepository paymentRepository;
    private final PaymentInformationService paymentInformationService;

    public StripeResponseDTO checkoutMonthlyStorageCost(String userId, Integer month, Integer year) {
        try {
            // Default to current month if not specified
            LocalDate now = LocalDate.now();
            int targetMonth = month != null ? month : now.getMonthValue();
            int targetYear = year != null ? year : now.getYear();

            // Get monthly storage cost from PaymentInformationService
            double monthlyStorageCost = paymentInformationService.calculateMonthlyStorageCost(userId, targetMonth, targetYear);
            BigDecimal totalAmount = new BigDecimal(monthlyStorageCost).setScale(2, RoundingMode.HALF_UP);

            // Apply minimum charge if necessary (Stripe requirement)
            if (totalAmount.compareTo(MINIMUM_CHARGE) < 0) {
                log.info("Monthly cost ${} is below minimum, applying minimum charge of ${}", totalAmount, MINIMUM_CHARGE);
                totalAmount = MINIMUM_CHARGE;
            }

            // Get current storage for display
            long currentStorageBytes = paymentInformationService.calculateTotalUserStorage(userId);
            double currentStorageMB = currentStorageBytes / (1024.0 * 1024.0);

            // Log the calculation for debugging
            log.info("User: {}, Month: {}/{}, Storage Cost: ${}, Current Storage: {:.2f}MB",
                    userId, targetMonth, targetYear, totalAmount, currentStorageMB);

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
                            .setName(String.format("Cloud Storage Bill - %s %d",
                                    LocalDate.of(targetYear, targetMonth, 1).getMonth().toString(), targetYear))
                            .setDescription(String.format("Monthly storage usage bill (%.2f MB used)", currentStorageMB))
                            .build();

            // Create price data
            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd") // Default to USD
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
                    .setSuccessUrl(baseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(baseUrl + "/payment/cancel")
                    .addLineItem(lineItem)
                    .putMetadata("userId", userId)
                    .putMetadata("month", String.valueOf(targetMonth))
                    .putMetadata("year", String.valueOf(targetYear))
                    .putMetadata("storageBytes", String.valueOf(currentStorageBytes))
                    .putMetadata("originalCost", String.valueOf(monthlyStorageCost))
                    .build();

            // Create Stripe session
            Session session = Session.create(params);

            // Save payment record to database
            Payment payment = Payment.builder()
                    .userId(userId)
                    .stripeSessionId(session.getId())
                    .storageGb((long) Math.ceil(currentStorageBytes / (1024.0 * 1024.0 * 1024.0))) // Convert to GB
                    .amount(totalAmount)
                    .currency("USD")
                    .status(Payment.PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);

            log.info("Payment session created for user: {} with session ID: {}", userId, session.getId());

            return StripeResponseDTO.builder()
                    .status("SUCCESS")
                    .message("Monthly storage bill checkout session created successfully")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error occurred: {}", e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment amount: {}", e.getMessage());
            throw new RuntimeException("Invalid payment amount: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during checkout: {}", e.getMessage(), e);
            throw new RuntimeException("Checkout failed: " + e.getMessage());
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

    // Get payment by session ID
    public Payment getPaymentBySessionId(String sessionId) {
        return paymentRepository.findByStripeSessionId(sessionId).orElse(null);
    }
}