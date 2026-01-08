package com.cognizant.smartpay.service;

import com.caen.RFIDLibrary.*;
import com.cognizant.smartpay.utility.TagForwarder;
import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class RFIDServiceNew1 {

    private CAENRFIDReader reader;
    private CAENRFIDLogicalSource logicalSource;

    @Autowired
    private TagForwarder tagForwarder;

    @PostConstruct
    public void init() {
        try {
            reader = new CAENRFIDReader();

            // Optional override via env var RFID_PORT or Java property rfid.port
            String configuredPort = System.getenv("RFID_PORT");
            if (configuredPort == null || configuredPort.isEmpty()) {
                configuredPort = System.getProperty("rfid.port");
            }
            if (configuredPort != null && !configuredPort.isEmpty()) {
                System.out.println("Configured RFID port override: " + configuredPort);
            }

            // List system-detected serial ports for debugging
            listSerialPorts();

            // Try connect: configured port first, then discovered ports, then COM1..COM20 fallback.
            if (!connectAndWait(configuredPort)) {
                System.err.println("RFID reader not found or not ready. Verify Device Manager, drivers and native DLL architecture.");
                return;
            }

            CAENRFIDReaderInfo info = reader.GetReaderInfo();
            System.out.println("Connected to CAEN RFID Reader: " + (info != null ? info.GetModel() : "unknown"));

            // logicalSource should be set by connectAndWait on success; if still null try to get it now
            if (logicalSource == null) {
                try {
                    logicalSource = reader.GetSource("Source_0");
                } catch (Exception e) {
                    System.err.println("Failed to obtain logical source after connect: " + e.getMessage());
                }
            }
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

    private void listSerialPorts() {
        try {
            SerialPort[] ports = SerialPort.getCommPorts();
            System.out.println("Detected serial ports:");
            for (SerialPort p : ports) {
                System.out.println(" - " + p.getSystemPortName() + " (" + p.getDescriptivePortName() + ")");
            }
        } catch (Throwable t) {
            System.out.println("jSerialComm not available or failed to list ports: " + t.getMessage());
        }
    }

    private boolean connectAndWait(String configuredPort) {
        // If configuredPort provided, try it first (both RS232 then USB)
        if (configuredPort != null && !configuredPort.isEmpty()) {
            if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_RS232, configuredPort)) return true;
            if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_USB, configuredPort)) return true;
        }

        // Try system-detected ports via jSerialComm
        try {
            SerialPort[] ports = SerialPort.getCommPorts();
            for (SerialPort sp : ports) {
                String name = sp.getSystemPortName();
                // For COM numbers > 9, the CAEN native lib may expect \\.\COMx; try both forms
                String alt = name.toUpperCase().startsWith("COM") && name.length() > 3 ? "\\\\.\\" + name : name;
                if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_RS232, name)) return true;
                if (!alt.equals(name) && tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_RS232, alt)) return true;
                if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_USB, name)) return true;
                if (!alt.equals(name) && tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_USB, alt)) return true;
            }
        } catch (Throwable t) {
            System.out.println("Skipping jSerialComm discovery: " + t.getMessage());
        }

        // Fallback COM1..COM20 using legacy scan
        String[] fallback = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> i > 9 ? "\\\\.\\" + "COM" + i : "COM" + i)
                .toArray(String[]::new);

        for (String port : fallback) {
            if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_RS232, port)) return true;
            if (tryConnectAndWaitForReady(CAENRFIDPort.CAENRFID_USB, port)) return true;
        }

        return false;
    }

    private boolean tryConnectAndWaitForReady(CAENRFIDPort portType, String port) {
        try {
            System.out.println("Trying " + (portType == CAENRFIDPort.CAENRFID_RS232 ? "RS232" : "USB") + " on " + port);
            reader.Connect(portType, port);

            // After Connect, the reader firmware may need time. Poll GetReaderInfo for up to 10 seconds.
            final int maxAttempts = 20;
            final long sleepMillis = 500;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    CAENRFIDReaderInfo info = reader.GetReaderInfo();
                    if (info != null) {
                        System.out.println("Connected and ready on " + port + " (model: " + info.GetModel() + ")");
                        // obtain logical source now to avoid later NPEs
                        try {
                            logicalSource = reader.GetSource("Source_0");
                            if (logicalSource == null) {
                                System.out.println("Warning: 'Source_0' not found immediately after connect on " + port);
                            }
                        } catch (Exception e) {
                            System.out.println("Could not get logical source immediately: " + e.getMessage());
                        }
                        return true;
                    }
                } catch (CAENRFIDException e) {
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
