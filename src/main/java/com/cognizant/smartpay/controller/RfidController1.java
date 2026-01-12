package com.cognizant.smartpay.controller;


import com.cognizant.smartpay.service.CaenRfidService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rfid")
public class RfidController1 {

    private final CaenRfidService rfidService;

    public RfidController1(CaenRfidService rfidService) {
        this.rfidService = rfidService;
    }

    @GetMapping("/scan")
    public List<String> scanTags() {
        return rfidService.readTags();
    }
}
