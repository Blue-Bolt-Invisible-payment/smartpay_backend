package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.dto.*;
import com.cognizant.smartpay.entity.User;
import com.cognizant.smartpay.service.BiometricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
<<<<<<< HEAD
import org.springframework.jdbc.core.JdbcTemplate;
=======
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for biometric authentication
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
<<<<<<< HEAD

@RequiredArgsConstructor
//@Slf4j
public class BiometricAuthController {

    private final JdbcTemplate jdbcTemplate;
    private final BiometricService biometricService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BiometricAuthController.class);

    /**
     * Authenticate user with fingerprint
     *
=======
@Slf4j
@RequiredArgsConstructor
public class BiometricAuthController {

    private final BiometricService biometricService;

    /**
     * Authenticate user with fingerprint
     * 
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
     * POST /api/auth/fingerprint
     */
    @PostMapping("/fingerprint")
    public ResponseEntity<UserDTO> authenticateFingerprint(
            @Valid @RequestBody FingerprintAuthRequest request) {
<<<<<<< HEAD

        log.info("Received fingerprint authentication request");

        try {
            User user = biometricService.authenticateFingerprint(request);

            UserDTO userDTO = convertToDTO(user);

            return ResponseEntity.ok(userDTO);

=======
        
        log.info("Received fingerprint authentication request");
        
        try {
            User user = biometricService.authenticateFingerprint(request);
            
            UserDTO userDTO = convertToDTO(user);
            
            return ResponseEntity.ok(userDTO);
            
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
        } catch (Exception e) {
            log.error("Authentication failed", e);
            throw e;
        }
    }

    /**
     * Enroll new fingerprint
<<<<<<< HEAD
     *
=======
     * 
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
     * POST /api/auth/enroll
     */
    @PostMapping("/enroll")
    public ResponseEntity<EnrollmentResponse> enrollFingerprint(
            @Valid @RequestBody EnrollmentRequest request) {
<<<<<<< HEAD

        log.info("Received enrollment request for user: {}", request);

        EnrollmentResponse response = biometricService.enrollFingerprint(request);

=======
        
        log.info("Received enrollment request for user: {}", request.getUserId());
        
        EnrollmentResponse response = biometricService.enrollFingerprint(request);
        
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Register new user with biometric enrollment
<<<<<<< HEAD
     *
=======
     * 
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<com.cognizant.smartpay.dto.RegistrationResponse> registerUser(
            @Valid @RequestBody com.cognizant.smartpay.dto.RegistrationRequest request) {
<<<<<<< HEAD

        log.info("Received registration request for: {}", request);

        try {
            User user = biometricService.registerNewUser(request);

=======
        
        log.info("Received registration request for: {}", request.getEmail());
        
        try {
            User user = biometricService.registerNewUser(request);
            
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
            com.cognizant.smartpay.dto.RegistrationResponse response = com.cognizant.smartpay.dto.RegistrationResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .walletBalance(user.getWalletBalance())
                .biometricEnabled(user.getBiometricEnabled())
                .status(user.getStatus())
                .message("User registered successfully")
                .build();
<<<<<<< HEAD

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

=======
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
        } catch (IllegalArgumentException e) {
            log.error("Registration validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Registration failed", e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Get all registered credential IDs for WebAuthn authentication
<<<<<<< HEAD
     *
=======
     * 
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
     * GET /api/auth/credentials
     */
    @GetMapping("/credentials")
    public ResponseEntity<?> getCredentials() {
        try {
            java.util.List<String> credentials = biometricService.getAllActiveCredentials();
            return ResponseEntity.ok(credentials);
        } catch (Exception e) {
            log.error("Failed to get credentials", e);
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    /**
     * Health check endpoint
<<<<<<< HEAD
     *
=======
     * 
>>>>>>> c2166c9f223089f1caeaf658a2a0e362a025065e
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("SmartPay Backend is running!");
    }

    /**
     * Convert User entity to UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getUserId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setWalletBalance(user.getWalletBalance());
        dto.setBiometricEnabled(user.getBiometricEnabled());
        dto.setEnabled(user.getEnabled());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
