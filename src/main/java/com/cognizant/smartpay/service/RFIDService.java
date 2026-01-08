package com.cognizant.smartpay.service;

import com.caen.RFIDLibrary.*;
import com.cognizant.smartpay.utility.TagForwarder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RFIDService {


    /*private CAENRFIDReader reader;
    private CAENRFIDLogicalSource logicalSource;

    @Autowired
    private TagForwarder tagForwarder;

    @PostConstruct
    public void init() {
        try {
           reader = new CAENRFIDReader();
            // Connect to the reader (e.g., via USB or Ethernet)

             reader.Connect(CAENRFIDPort.CAENRFID_USB, "COM5");
            //reader.Connect(CAENRFIDPort.CAENRFID_RS232, "COM1");
            //reader.Connect(CAENRFIDPort.CAENRFID_TCP,"192.168.0.2");


            CAENRFIDReaderInfo info = reader.GetReaderInfo();
            System.out.println("Connected to CAEN RFID Reader: " + info.GetModel());

            logicalSource = reader.GetSource("Source_0");
            // Configure logical source if needed (e.g., antenna power)
            // logicalSource.SetPower(25); // Example: Set power to 25 dBm
            inventoryTags();

        } catch (CAENRFIDException e) {
            System.err.println("Error initializing CAEN RFID reader: " + e.getMessage());
        }
    }

    public void inventoryTags() {
        List<String> epcTags = new ArrayList<>();
        try {
            CAENRFIDTag[] tags = logicalSource.InventoryTag();
            if (tags != null) {
                for (CAENRFIDTag tag : tags) {
                    epcTags.add(tag.GetId().toString());
                }
            }

            tagForwarder.forwardTag(epcTags);

        } catch(CAENRFIDException e)
        {
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
    }*/

}
