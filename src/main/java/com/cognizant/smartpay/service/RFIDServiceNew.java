package com.cognizant.smartpay.service;

import com.caen.RFIDLibrary.*;
import com.cognizant.smartpay.utility.TagForwarder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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

            // Try scanning RS232 ports; if none succeed, abort init (or switch to TCP fallback here)
            if (!connectRs232()) {
                System.err.println("RS232 connect failed for scanned ports. Verify Device Manager, drivers and native DLL architecture.");
                return;
            }

            CAENRFIDReaderInfo info = reader.GetReaderInfo();
            System.out.println("Connected to CAEN RFID Reader: " + info.GetModel());

            logicalSource = reader.GetSource("Source_0");
            inventoryTags();

        } catch (CAENRFIDException e) {
            System.err.println("Error initializing CAEN RFID reader: " + e.getMessage());
        }
    }

    private boolean connectRs232() {
        // Scan COM1..COM20. Use "\\\\.\\COMx" for x > 9 (Java literal yields \\.\COMx at runtime).
        String[] ports = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> i > 9 ? "\\\\.\\" + "COM" + i : "COM" + i) // results like "\\\\.\\COM10"
                .toArray(String[]::new);

        for (String port : ports) {
            try {
                System.out.println("Trying RS232 port: " + port);
                reader.Connect(CAENRFIDPort.CAENRFID_RS232, port);
                System.out.println("Connected to CAEN RFID on " + port);
                return true;
            } catch (CAENRFIDException e) {
                System.out.println("RS232 connect failed for " + port + ": " + e.getMessage());
            } catch (UnsatisfiedLinkError lle) {
                System.err.println("Native library error: " + lle.getMessage() + " — check CAEN native DLL presence and 32/64-bit match.");
                break; // native DLL missing or wrong arch; stop scanning
            } catch (Exception ex) {
                System.err.println("Unexpected error on " + port + ": " + ex.getMessage());
            }
        }
        return false;
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
