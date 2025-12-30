package com.cognizant.smartpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for User response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private BigDecimal walletBalance;
    private Boolean biometricEnabled;
    private Boolean enabled;
    private String status;
}
