package com.shareApp.Payment.controller;

import com.shareApp.Payment.dto.PaymentRequestDTO;
import com.shareApp.Payment.dto.StripeResponseDTO;
import com.shareApp.Payment.services.StripeService;
import com.shareApp.Utils.security.JWTService;
import com.shareApp.Utils.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    private final StripeService stripeService;
    private final JWTService jwtService;

    public PaymentController(StripeService stripeService, JWTService jwtService) {
        this.stripeService = stripeService;
        this.jwtService = jwtService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<StripeResponseDTO> checkoutProducts(
            @RequestHeader("Authorization") String authToken,
            @RequestBody PaymentRequestDTO paymentRequest) {
        log.info("Entered into checkout ---------1----- ");
        try {
            log.info("Entered into checkout ---------try----- ");
            String userId = jwtService.validateTokenAndGetUserId(authToken);
            log.info("Entered into checkout ---------under userid ----- ");
            StripeResponseDTO stripeResponse = stripeService.checkoutProducts(userId, paymentRequest);
            return ResponseEntity.ok(stripeResponse);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(StripeResponseDTO.builder()
                            .status("ERROR")
                            .message("Unauthorized: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(StripeResponseDTO.builder()
                            .status("ERROR")
                            .message("Payment processing failed: " + e.getMessage())
                            .build());
        }
    }
}