package com.shareApp.Payment.controller;

import com.shareApp.Payment.entitites.Payment;
import com.shareApp.Payment.services.StripeService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final StripeService stripeService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Received Stripe webhook");

        try {
            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Processing event: {}", event.getType());

            // Handle events with individual try-catch to prevent 500s
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompletedSafely(event);
                    break;

                case "checkout.session.expired":
                    handleCheckoutSessionExpiredSafely(event);
                    break;

                case "payment_intent.succeeded":
                    handlePaymentIntentSucceededSafely(event);
                    break;

                case "payment_intent.payment_failed":
                    handlePaymentIntentFailedSafely(event);
                    break;

                default:
                    log.info("Unhandled event type: {}", event.getType());
            }

            // Always return 200 to Stripe (even if individual handlers have issues)
            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            // Return 400 for signature issues (tells Stripe not to retry)
            return ResponseEntity.badRequest().body("Invalid signature");
        }
    }

    private void handleCheckoutSessionCompletedSafely(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (session == null) {
                log.warn("Session is null in checkout.session.completed");
                return;
            }

            String sessionId = session.getId();
            if (sessionId == null) {
                log.warn("Session ID is null");
                return;
            }

            // Safely get metadata
            String userId = null;
            String month = null;
            String year = null;

            if (session.getMetadata() != null) {
                userId = session.getMetadata().get("userId");
                month = session.getMetadata().get("month");
                year = session.getMetadata().get("year");
            }

            log.info("Checkout completed - Session: {}, User: {}, Period: {}/{}",
                    sessionId, userId, month, year);

            // Update payment status
            stripeService.updatePaymentStatus(sessionId, Payment.PaymentStatus.COMPLETED);
            log.info("✅ Payment status updated to COMPLETED for session: {}", sessionId);

        } catch (Exception e) {
            log.error("Error in checkout.session.completed handler: {}", e.getMessage());
            // Don't re-throw - we want to return 200 to Stripe
        }
    }

    private void handleCheckoutSessionExpiredSafely(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (session != null && session.getId() != null) {
                stripeService.updatePaymentStatus(session.getId(), Payment.PaymentStatus.CANCELLED);
                log.info("Payment session expired: {}", session.getId());
            }
        } catch (Exception e) {
            log.error("Error in checkout.session.expired handler: {}", e.getMessage());
            // Don't re-throw
        }
    }

    private void handlePaymentIntentSucceededSafely(Event event) {
        try {
            log.info("Payment intent succeeded: {}", event.getId());
        } catch (Exception e) {
            log.error("Error in payment_intent.succeeded handler: {}", e.getMessage());
        }
    }

    private void handlePaymentIntentFailedSafely(Event event) {
        try {
            log.warn("Payment intent failed: {}", event.getId());
        } catch (Exception e) {
            log.error("Error in payment_intent.failed handler: {}", e.getMessage());
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testWebhook() {
        return ResponseEntity.ok("Webhook endpoint is active");
    }
}