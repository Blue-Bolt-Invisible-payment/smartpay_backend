package com.cognizant.smartpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Class for SmartPay Backend
 * 
 * @author Cognizant SmartPay Team
 * @version 1.0.0
 */
@SpringBootApplication
public class SmartPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPayApplication.class, args);
        System.out.println("===========================================");
        System.out.println("SmartPay Backend Started Successfully!");
        System.out.println("Server running on: http://localhost:8080");
        System.out.println("===========================================");
    }
}
