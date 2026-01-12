package com.cognizant.smartpay.service;

import com.caen.RFIDLibrary.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import com.caen.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaenRfidService {

    private CAENRFIDReader reader;
    private CAENRFIDLogicalSource source;

    private final Map<String, Long> scannedTags = new ConcurrentHashMap<>();

    @PostConstruct
    public synchronized void init() {
        connectReader();
    }

    private void connectReader() {
        try {
            reader = new CAENRFIDReader();
            reader.Connect(CAENRFIDPort.CAENRFID_USB, "COM5");

            // 🔑 FIX: wait until reader is READY
            Thread.sleep(1500);

            source = reader.GetSources()[0];


            // Enable antenna
            //source.SetEnabledAntennas(new short[]{1});
            //source.SetPower(30);
            reader.SetPower(30);
            source.SetReadCycle(1000);

            startInventory();

            System.out.println("CAEN RFID Reader connected");

        } catch (Exception e) {
            System.err.println("RFID init failed: " + e.getMessage());
            retryLater();
        }
    }

    private void startInventory() throws CAENRFIDException {
        //source.InventoryAbort();
        reader.InventoryAbort();
        source.InventoryTag();
    }

    private void retryLater() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                connectReader();
            }
        }, 5000);
    }

    public List<String> readTags() {
        try {
            if (reader == null ) {
                connectReader();
                return List.of();
            }

            CAENRFIDTag[] tags = source.InventoryTag();

            for (CAENRFIDTag tag : tags) {
                scannedTags.put(tag.GetId().toString(), System.currentTimeMillis());
            }

            return new ArrayList<>(scannedTags.keySet());

        } catch (Exception e) {
            System.err.println("Read failed: " + e.getMessage());
            return List.of();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (source != null) reader.InventoryAbort();/*source.InventoryAbort()*/;
            if (reader != null) reader.Disconnect();
        } catch (Exception ignored) {}
    }
}