package com.cognizant.smartpay.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class TagForwarder {

    @Autowired
    private RestTemplate restTemplate;

    public void forwardTag(List<String> tag) {

        String url = "http://localhost:8080/api/rfid/read";
        restTemplate.postForObject(url, tag, Void.class);
    }
}
