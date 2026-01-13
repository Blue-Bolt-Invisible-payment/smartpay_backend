
package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
<<<<<<< HEAD
@CrossOrigin(origins = "http://localhost:3001")
=======
@CrossOrigin(origins = "http://localhost:3000")
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Processing payment for user: {}", userId);
        try {
            Map<String, Object> result = paymentService.processPayment(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "status", "failed"));
        } catch (Exception e) {
<<<<<<< HEAD
            e.printStackTrace(); // This prints the error to your terminal
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(), // This sends the ACTUAL error to your React app
                    "status", "failed"
            ));
=======
            return ResponseEntity.internalServerError().body(Map.of("error", "Payment failed", "status", "failed"));
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
        }
    }
}
