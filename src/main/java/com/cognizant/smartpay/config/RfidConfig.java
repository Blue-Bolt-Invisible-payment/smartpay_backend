package com.cognizant.smartpay.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RfidConfig {

    @PostConstruct
    public void loadLibrary() {
        System.loadLibrary("caenrfid"); // caenrfid.dll / libcaenrfid.so
        System.setProperty("caen.rfid.debug", "true");
    }
}
