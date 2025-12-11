package com.cognizant.smartpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for user registration response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private BigDecimal walletBalance;
    private Boolean biometricEnabled;
    private String status;
    private String message;
}
