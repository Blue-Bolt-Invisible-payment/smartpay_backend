package com.cognizant.smartpay.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public class RFIDController {

    //private RFIDAddCartService rfidAddCartService;

    @PostMapping("/read")
    public ResponseEntity<String> onTagRead(@RequestBody List<String> tag) {



       /* rfidAddCartService.addItemToCart(tag);*/
        System.out.println("*********************************************************");
        System.out.println("******************************Scanned Tags*******************************");
        System.out.println("RFID Tag Received from Scanner: " + tag);
        System.out.println("*********************************************************");

        return ResponseEntity.ok("Received EPC: " + tag);
    }
}
