package com.cognizant.smartpay.controller;

import com.cognizant.smartpay.service.RFIDScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
@RequestMapping("/api/scan")
public class RFIDScanController {

    @Autowired
    RFIDScanService rfidScanService;

    @PostMapping("/addtocart")
    public ResponseEntity<?> addScannedProudctToCart(@RequestBody List<String> scannedTags){
        rfidScanService.processScannedTags(scannedTags);
        return ResponseEntity.ok(Map.of("success", true, "message", "scanned tag processed success fully"));
    }

}
