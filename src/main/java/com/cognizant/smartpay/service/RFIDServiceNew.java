package com.cognizant.smartpay.service;

import com.caen.RFIDLibrary.*;
import com.cognizant.smartpay.utility.TagForwarder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Service
public class RFIDServiceNew {


    private CAENRFIDReader reader;
    private CAENRFIDLogicalSource logicalSource;

    @Autowired
    private TagForwarder tagForwarder;

    @PostConstruct
    public void init() {
        try {
            reader = new CAENRFIDReader();

            // Try scanning COM1..COM20 and attempt RS232 first, then USB fallback.
            if (!connectAndWait()) {
                System.err.println("RFID reader not found or not ready. Verify Device Manager, drivers and native DLL architecture.");
                // Do not throw here to allow Spring context to start; service will be inactive.
                return;
            }

            CAENRFIDReaderInfo info = reader.GetReaderInfo();
            System.out.println("Connected to CAEN RFID Reader: " + info.GetModel());

            logicalSource = reader.GetSource("Source_0");
            if (logicalSource == null) {
                System.err.println("Logical source 'Source_0' not found on reader.");
                return;
            }

            inventoryTags();

        } catch (UnsatisfiedLinkError lle) {
            System.err.println("Native CAEN DLL error: " + lle.getMessage() + " — check native DLL presence and 32/64-bit match.");
        } catch (CAENRFIDException e) {
            System.err.println("Error initializing CAEN RFID reader: " + e.getMessage());
        } catch (Exception ex) {
            System.err.println("Unexpected init error: " + ex.getMessage());
        }
    }

    // Scans COM1..COM20. For each port try RS232 then USB. After Connect, wait until GetReaderInfo succeeds (small retry loop).
    private boolean connectAndWait() {
        String[] ports = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> i > 9 ? "\\\\.\\" + "COM" + i : "COM" + i)
                .toArray(String[]::new);

        for (String port : ports) {
            // Try RS232
           // if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_RS232, port)) return true;
            // Try USB (some CAEN drivers expose USB as COM)
            if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_USB, port)) return true;
        }
        return false;
    }

    private boolean tryConnectAndWaitForReady(CAENRFIDPort portType, String port) {
        try {
            System.out.println("Trying " + (portType == CAENRFIDPort.CAENRFID_RS232 ? "RS232" : "USB") + " on " + port);
            reader.Connect(portType, port);

            // After Connect, the reader firmware may need a short time to become ready. Poll GetReaderInfo for up to 5 seconds.
            final int maxAttempts = 10;
            final long sleepMillis = 500;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    CAENRFIDReaderInfo info = reader.GetReaderInfo();
                    if (info != null) {
                        System.out.println("Connected and ready on " + port);
                        return true;
                    }
                } catch (CAENRFIDException e) {
                    // Not ready yet; continue retrying
                    System.out.println("Reader not ready yet on " + port + " (attempt " + attempt + "): " + e.getMessage());
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // If not ready within timeout, disconnect and continue scanning
            try {
                reader.Disconnect();
            } catch (Exception ignore) { }
            System.out.println("Timed out waiting for reader readiness on " + port);
            return false;

        } catch (CAENRFIDException e) {
            System.out.println("Connect failed on " + port + ": " + e.getMessage());
            return false;
        } catch (UnsatisfiedLinkError lle) {
            System.err.println("Native library error while connecting: " + lle.getMessage());
            // stop scanning further because native DLL missing/wrong arch
            throw lle;
        } catch (Exception ex) {
            System.err.println("Unexpected error while connecting on " + port + ": " + ex.getMessage());
            return false;
        }
    }

    public void inventoryTags() {
        List<String> epcTags = new ArrayList<>();
        try {
            if (logicalSource == null) {
                System.err.println("Logical source is null; cannot inventory tags.");
                return;
            }
            CAENRFIDTag[] tags = logicalSource.InventoryTag();
            if (tags != null) {
                for (CAENRFIDTag tag : tags) {
                    epcTags.add(tag.GetId().toString());
                }
            }
            tagForwarder.forwardTag(epcTags);
        } catch (CAENRFIDException e) {
            System.err.println("Error during tag inventory: " + e.getMessage());
        } catch (Exception ex) {
            System.err.println("Unexpected inventory error: " + ex.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (reader != null) {
            try {
                reader.Disconnect();
                System.out.println("Disconnected from CAEN RFID Reader.");
            } catch (CAENRFIDException e) {
                System.err.println("Error disconnecting from CAEN RFID reader: " + e.getMessage());
            }
        }
    }

}
