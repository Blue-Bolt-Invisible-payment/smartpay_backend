package com.cognizant.smartpay.service;

import com.cognizant.smartpay.dto.EnrollmentRequest;
import com.cognizant.smartpay.dto.EnrollmentResponse;
import com.cognizant.smartpay.dto.FingerprintAuthRequest;
import com.cognizant.smartpay.entity.Biometric;
import com.cognizant.smartpay.entity.User;
import com.cognizant.smartpay.entity.Wallet;
import com.cognizant.smartpay.exception.AuthenticationFailedException;
import com.cognizant.smartpay.exception.BiometricNotFoundException;
import com.cognizant.smartpay.repository.BiometricRepository;
import com.cognizant.smartpay.repository.UserRepository;
import com.cognizant.smartpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for biometric authentication operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BiometricService {

    private final BiometricRepository biometricRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    /**
     * Authenticate user using fingerprint data
     */
    @Transactional
    public User authenticateFingerprint(FingerprintAuthRequest request) {
        log.debug("Starting fingerprint authentication");
        
        Map<String, Object> fingerprintData = request.getFingerprintData();
        Map<String, Object> deviceInfo = request.getDeviceInfo();
        
        String method = (String) deviceInfo.get("method");
        
        User user;
        if ("webauthn".equals(method)) {
            user = authenticateWebAuthn(fingerprintData);
        } else if ("external_usb".equals(method)) {
            user = authenticateExternalScanner(fingerprintData);
        } else {
            throw new IllegalArgumentException("Unsupported authentication method: " + method);
        }
        
        if (user == null || !user.getEnabled()) {
            throw new AuthenticationFailedException("Authentication failed. User not found or disabled.");
        }
        
        // Get wallet balance
        Optional<Wallet> walletOpt = walletRepository.findByUserId(user.getUserId());
        walletOpt.ifPresent(wallet -> user.setWalletBalance(wallet.getBalance()));
        
        log.info("User authenticated successfully: {}", user.getEmail());
        return user;
    }

    /**
     * Authenticate using WebAuthn (Mobile/Laptop)
     */
    private User authenticateWebAuthn(Map<String, Object> fingerprintData) {
        log.debug("Authenticating via WebAuthn");
        
        try {
            String credentialId = (String) fingerprintData.get("credentialId");
            
            if (credentialId == null || credentialId.isEmpty()) {
                throw new AuthenticationFailedException("Credential ID is required");
            }
            
            log.info("Attempting to authenticate with credential ID: {}", credentialId);
            
            // Find biometric record by credential ID
            Optional<Biometric> biometricOpt = biometricRepository
                .findByCredentialIdAndIsActive(credentialId, true);
            
            if (!biometricOpt.isPresent()) {
                log.warn("Credential not found: {}. Checking all active credentials...", credentialId);
                // Debug: List all active credentials
                List<Biometric> allBiometrics = biometricRepository.findAllByIsActive(true);
                log.info("Total active biometrics: {}", allBiometrics.size());
                for (Biometric b : allBiometrics) {
                    log.info("Stored credential ID: {}", b.getCredentialId());
                }
                throw new BiometricNotFoundException("Credential not found or inactive");
            }
            
            Biometric biometric = biometricOpt.get();
            
            // In production, verify signature here using WebAuthn library
            // For now, we trust the credential ID match
            boolean verified = verifyWebAuthnSignature(fingerprintData, biometric);
            
            if (!verified) {
                throw new AuthenticationFailedException("Signature verification failed");
            }
            
            // Update verification stats
            biometric.setLastVerifiedAt(LocalDateTime.now());
            biometric.setVerificationCount(biometric.getVerificationCount() + 1);
            biometricRepository.save(biometric);
            
            // Get user
            User user = userRepository.findById(biometric.getUserId())
                .orElseThrow(() -> new BiometricNotFoundException("User not found"));
            
            return user;
            
        } catch (Exception e) {
            log.error("WebAuthn authentication failed", e);
            throw new AuthenticationFailedException("WebAuthn authentication failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate using external USB scanner
     */
    private User authenticateExternalScanner(Map<String, Object> fingerprintData) {
        log.debug("Authenticating via external scanner");
        
        try {
            String templateBase64 = (String) fingerprintData.get("fingerprintTemplate");
            
            if (templateBase64 == null || templateBase64.isEmpty()) {
                throw new AuthenticationFailedException("Fingerprint template is required");
            }
            
            byte[] capturedTemplate = Base64.getDecoder().decode(templateBase64);
            
            // Get all active enrolled fingerprints
            List<Biometric> enrolledBiometrics = biometricRepository.findAllByIsActive(true);
            
            if (enrolledBiometrics.isEmpty()) {
                throw new BiometricNotFoundException("No enrolled fingerprints found");
            }
            
            // Match against each enrolled fingerprint
            for (Biometric biometric : enrolledBiometrics) {
                byte[] storedTemplate = biometric.getFingerprintTemplate();
                
                // Simple matching algorithm (in production, use vendor SDK)
                int matchScore = matchTemplates(capturedTemplate, storedTemplate);
                
                log.debug("Match score for user {}: {}", biometric.getUserId(), matchScore);
                
                // Threshold: 85%
                if (matchScore >= 85) {
                    // Match found!
                    log.info("Fingerprint matched for user: {}", biometric.getUserId());
                    
                    biometric.setLastVerifiedAt(LocalDateTime.now());
                    biometric.setVerificationCount(biometric.getVerificationCount() + 1);
                    biometricRepository.save(biometric);
                    
                    User user = userRepository.findById(biometric.getUserId())
                        .orElseThrow(() -> new BiometricNotFoundException("User not found"));
                    
                    return user;
                }
            }
            
            // No match found
            log.warn("No matching fingerprint found");
            throw new AuthenticationFailedException("Fingerprint not recognized");
            
        } catch (Exception e) {
            log.error("External scanner authentication failed", e);
            throw new AuthenticationFailedException("Scanner authentication failed: " + e.getMessage());
        }
    }

    /**
     * Verify WebAuthn signature (simplified version)
     */
    private boolean verifyWebAuthnSignature(Map<String, Object> fingerprintData, Biometric biometric) {
        // In production, use Yubico WebAuthn library to verify ECDSA signature
        // For now, we assume credential ID match is sufficient
        
        // This is a placeholder - implement full WebAuthn verification in production
        String signature = (String) fingerprintData.get("signature");
        String authenticatorData = (String) fingerprintData.get("authenticatorData");
        
        return signature != null && authenticatorData != null;
    }

    /**
     * Match fingerprint templates (simplified version)
     */
    private int matchTemplates(byte[] template1, byte[] template2) {
        // This is a simplified matching algorithm
        // In production, use vendor SDK (DigitalPersona, Mantra, etc.)
        
        if (template1 == null || template2 == null) {
            return 0;
        }
        
        if (template1.length != template2.length) {
            return 0;
        }
        
        // Simple byte-by-byte comparison
        int matches = 0;
        for (int i = 0; i < template1.length; i++) {
            if (template1[i] == template2[i]) {
                matches++;
            }
        }
        
        return (int) ((double) matches / template1.length * 100);
    }

    /**
     * Enroll new fingerprint
     */
    @Transactional
    public EnrollmentResponse enrollFingerprint(EnrollmentRequest request) {
        log.debug("Enrolling fingerprint for user: {}", request.getUserId());
        
        // Verify user exists
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new BiometricNotFoundException("User not found"));
        
        Map<String, Object> fingerprintData = request.getFingerprintData();
        Map<String, Object> deviceInfo = request.getDeviceInfo();
        
        Biometric biometric = new Biometric();
        biometric.setUserId(user.getUserId());
        biometric.setDeviceType((String) deviceInfo.get("deviceType"));
        biometric.setEnrollmentMethod((String) deviceInfo.get("method"));
        
        String method = (String) deviceInfo.get("method");
        
        if ("webauthn".equals(method)) {
            // Store WebAuthn credential
            String credentialId = (String) fingerprintData.get("credentialId");
            String publicKeyB64 = (String) fingerprintData.get("publicKey");
            
            biometric.setCredentialId(credentialId);
            
            if (publicKeyB64 != null) {
                biometric.setPublicKey(Base64.getDecoder().decode(publicKeyB64));
            }
            
            // Generate hash
            biometric.setFingerprintHash(generateHash(credentialId));
            
            // Store dummy template for WebAuthn
            biometric.setFingerprintTemplate(new byte[0]);
            
        } else if ("external_usb".equals(method)) {
            // Store fingerprint template
            String templateB64 = (String) fingerprintData.get("fingerprintTemplate");
            byte[] template = Base64.getDecoder().decode(templateB64);
            
            biometric.setFingerprintTemplate(template);
            biometric.setFingerprintHash(generateHash(templateB64));
        }
        
        biometric.setIsActive(true);
        biometric.setVerificationCount(0);
        biometric.setEnrolledAt(LocalDateTime.now());
        
        biometric = biometricRepository.save(biometric);
        
        // Update user biometric status
        user.setBiometricEnabled(true);
        userRepository.save(user);
        
        log.info("Fingerprint enrolled successfully for user: {}", user.getEmail());
        
        return new EnrollmentResponse(
            biometric.getBiometricId(),
            "Fingerprint enrolled successfully",
            biometric.getDeviceType(),
            true
        );
    }

    /**
     * Register new user with biometric enrollment
     */
    @Transactional
    public User registerNewUser(com.cognizant.smartpay.dto.RegistrationRequest request) {
        log.info("Starting user registration for email: {}", request.getEmail());
        
        // Check if email already exists
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }
        
        // Check if phone number already exists (if phone is provided)
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            Optional<User> existingUserByPhone = userRepository.findByPhone(request.getPhone());
            if (existingUserByPhone.isPresent()) {
                throw new IllegalArgumentException("User with phone number " + request.getPhone() + " already exists");
            }
        }
        
        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setBiometricEnabled(false); // Will be set to true after biometric enrollment
        user.setEnabled(true);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        user = userRepository.save(user);
        log.info("User created with ID: {}", user.getUserId());
        
        // Create wallet
        Wallet wallet = new Wallet();
        wallet.setUserId(user.getUserId());
        wallet.setBalance(request.getInitialWalletBalance());
        wallet.setCreatedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        
        walletRepository.save(wallet);
        log.info("Wallet created for user: {}", user.getUserId());
        
        // Enroll biometric
        try {
            Biometric biometric = new Biometric();
            biometric.setUserId(user.getUserId());
            biometric.setDeviceType(request.getDeviceType());
            
            Map<String, Object> fingerprintData = request.getFingerprintData();
            String method = (String) fingerprintData.get("method");
            
            if ("webauthn".equals(method)) {
                String credentialId = (String) fingerprintData.get("credentialId");
                biometric.setCredentialId(credentialId);
                biometric.setEnrollmentMethod("webauthn");
                
                // Decode public key from base64 if present
                String publicKeyB64 = (String) fingerprintData.get("publicKey");
                if (publicKeyB64 != null && !publicKeyB64.isEmpty()) {
                    byte[] publicKey = Base64.getDecoder().decode(publicKeyB64);
                    biometric.setPublicKey(publicKey);
                } else {
                    biometric.setPublicKey(new byte[0]);
                }
                
                biometric.setFingerprintHash(generateHash(credentialId));
                biometric.setFingerprintTemplate(new byte[0]);
                
                log.info("Enrolled WebAuthn credential ID: {}", credentialId);
            } else if ("external_usb".equals(method)) {
                String templateB64 = (String) fingerprintData.get("fingerprintTemplate");
                byte[] template = Base64.getDecoder().decode(templateB64);
                biometric.setFingerprintTemplate(template);
                biometric.setFingerprintHash(generateHash(templateB64));
                biometric.setEnrollmentMethod("external_usb");
            } else {
                throw new IllegalArgumentException("Unsupported biometric method: " + method);
            }
            
            biometric.setIsActive(true);
            biometric.setVerificationCount(0);
            biometric.setEnrolledAt(LocalDateTime.now());
            
            biometricRepository.save(biometric);
            log.info("Biometric enrolled for user: {}", user.getUserId());
            
            // Update user biometric status
            user.setBiometricEnabled(true);
            user = userRepository.save(user);
            
        } catch (Exception e) {
            log.error("Biometric enrollment failed during registration", e);
            throw new RuntimeException("Failed to enroll biometric: " + e.getMessage());
        }
        
        // Set wallet balance in user object
        user.setWalletBalance(request.getInitialWalletBalance());
        
        log.info("User registration completed successfully for: {}", user.getEmail());
        return user;
    }

    /**
     * Get all active credential IDs for WebAuthn authentication
     */
    public List<String> getAllActiveCredentials() {
        List<Biometric> biometrics = biometricRepository.findAllByIsActive(true);
        return biometrics.stream()
            .map(Biometric::getCredentialId)
            .filter(id -> id != null && !id.isEmpty())
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Generate SHA-256 hash
     */
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating hash", e);
            return null;
        }
    }
}
