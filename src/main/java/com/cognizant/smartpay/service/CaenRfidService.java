package com.cognizant.smartpay.service;

import com.caen.RFIDLibrary.*;
import com.cognizant.smartpay.utility.TagForwarder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaenRfidService {

    private CAENRFIDReader reader;
    private CAENRFIDLogicalSource source;

    @Autowired
    private TagForwarder tagForwarder;

    private final Map<String, Long> scannedTags = new ConcurrentHashMap<>();

    @PostConstruct
    public synchronized void init() {
        connectReader();
    }

    private void connectReader() {
        try {
            reader = new CAENRFIDReader();
            // Connecting to the reader
            reader.Connect(CAENRFIDPort.CAENRFID_RS232, "COM11");

            // 🔑 FIX: Wait until reader is fully initialized
            Thread.sleep(1500);

            source = reader.GetSources()[0];
            CAENRFIDReaderInfo info = reader.GetReaderInfo();

            System.out.println("--- Connection Successful ---");
            System.out.println("Model: " + info.GetModel());
            System.out.println("Firmware: " + reader.GetFirmwareRelease());

            // Configuration
            source.SetReadCycle(1000);

            System.out.println("Starting continuous read... Press Ctrl+C to stop.");
            System.out.println("-------------------------------------------------");

            List<String> epcTags = new ArrayList<>();
            // Continuous Inventory Loop
            while (true) {
                CAENRFIDTag[] tags = source.InventoryTag();

                if (tags != null && tags.length > 0) {
                    for (CAENRFIDTag tag : tags) {
                        // FIX: Convert the byte ID to Hex String for consistency
                        String hexId = bytesToHex(tag.GetId());
                        System.out.println("Tag ID (Hex): " + hexId);
                        epcTags.add(hexId);
                    }
                }
                tagForwarder.forwardTag(epcTags);
                epcTags.clear();
                // Small sleep to prevent CPU spiking
                Thread.sleep(100);
            }

        } catch (CAENRFIDException e) {
            System.err.println("RFID Error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Process interrupted.");
        } finally {
            try {
                if (reader != null) {
                    reader.Disconnect();
                    System.out.println("Disconnected from reader.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            if (reader == null) {
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
            if (source != null) reader.InventoryAbort();/*source.InventoryAbort()*/
            ;
            if (reader != null) reader.Disconnect();
        } catch (Exception ignored) {
        }
    }

    // Helper method to convert byte array to a readable Hex String
    public String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}