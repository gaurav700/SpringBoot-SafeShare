package com.shareApp.Payment.controller;

import com.shareApp.Payment.dto.StripeResponseDTO;
import com.shareApp.Payment.entitites.Payment;
import com.shareApp.Payment.services.StripeService;
import com.shareApp.Utils.security.JWTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final StripeService stripeService;
    private final JWTService jwtService;

    @PostMapping("/checkout")
    public ResponseEntity<StripeResponseDTO> checkoutStorageBill(
            @RequestHeader("Authorization") String authToken,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        try {
            String userId = jwtService.validateTokenAndGetUserId(authToken);

            log.info("Processing checkout for user: {}, month: {}, year: {}", userId, month, year);

            StripeResponseDTO response = stripeService.checkoutMonthlyStorageCost(userId, month, year);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Checkout failed: {}", e.getMessage(), e);

            StripeResponseDTO errorResponse = StripeResponseDTO.builder()
                    .status("ERROR")
                    .message("Checkout failed: " + e.getMessage())
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> paymentSuccess(
            @RequestParam("session_id") String sessionId
    ) {
        try {
            // Update payment status to completed
            stripeService.updatePaymentStatus(sessionId, Payment.PaymentStatus.COMPLETED);

            // Get payment details
            Payment payment = stripeService.getPaymentBySessionId(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Payment completed successfully!");
            response.put("sessionId", sessionId);

            if (payment != null) {
                response.put("amount", payment.getAmount());
                response.put("currency", payment.getCurrency());
                response.put("userId", payment.getUserId());
            }

            log.info("Payment successful for session: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing payment success: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Error processing payment success");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<Map<String, Object>> paymentCancel() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "CANCELLED");
        response.put("message", "Payment was cancelled by user");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{sessionId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String authToken
    ) {
        try {
            String userId = jwtService.validateTokenAndGetUserId(authToken);
            Payment payment = stripeService.getPaymentBySessionId(sessionId);

            if (payment == null || !payment.getUserId().equals(userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("message", "Payment not found or access denied");

                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("status", payment.getStatus());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("storageGb", payment.getStorageGb());
            response.put("createdAt", payment.getCreatedAt());
            response.put("updatedAt", payment.getUpdatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting payment status: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Error retrieving payment status");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}