package com.cognizant.smartpay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for fingerprint authentication request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FingerprintAuthRequest {

    @NotNull(message = "Fingerprint data is required")
    private Map<String, Object> fingerprintData;

    @NotNull(message = "Device info is required")
    private Map<String, Object> deviceInfo;
}
