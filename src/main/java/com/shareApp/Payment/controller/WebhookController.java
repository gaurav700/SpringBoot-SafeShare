package com.shareApp.Payment.controller;

import com.shareApp.Payment.entitites.Payment;
import com.shareApp.Payment.services.StripeService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@Slf4j
public class WebhookController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    private final StripeService stripeService;

    public WebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            
            switch (event.getType()) {
                case "checkout.session.completed":
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (session != null) {
                        stripeService.updatePaymentStatus(session.getId(), Payment.PaymentStatus.COMPLETED);
                    }
                    break;
                case "checkout.session.expired":
                    Session expiredSession = (Session) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (expiredSession != null) {
                        stripeService.updatePaymentStatus(expiredSession.getId(), Payment.PaymentStatus.CANCELLED);
                    }
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }
            
            return ResponseEntity.ok("Webhook handled successfully");
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Webhook error");
        }
    }
}
