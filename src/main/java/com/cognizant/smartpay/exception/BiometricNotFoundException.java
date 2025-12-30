package com.cognizant.smartpay.exception;

/**
 * Exception thrown when biometric data is not found
 */
public class BiometricNotFoundException extends RuntimeException {

    public BiometricNotFoundException(String message) {
        super(message);
    }

    public BiometricNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
