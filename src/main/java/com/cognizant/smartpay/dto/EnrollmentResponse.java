package com.cognizant.smartpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for enrollment response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long biometricId;
    private String message;
    private String deviceType;
    private Boolean success;

    public EnrollmentResponse(Long biometricId, String message) {
        this.biometricId = biometricId;
        this.message = message;
        this.success = true;
    }
}
