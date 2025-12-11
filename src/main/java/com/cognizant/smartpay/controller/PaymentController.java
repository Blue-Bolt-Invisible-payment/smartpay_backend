package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for payment operations
 */
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process payment for user's cart
     * POST /api/payment/process
     */
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");

        log.info("Processing payment request for user: {}", userId);

        try {
            Map<String, Object> result = paymentService.processPayment(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Payment validation error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "status", "failed"
            ));
        } catch (Exception e) {
            log.error("Payment processing error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Payment processing failed",
                "status", "failed"
            ));
        }
    }
}