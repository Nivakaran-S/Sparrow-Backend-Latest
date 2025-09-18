package com.sparrow.parcel_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparrow.parcel_service.model.Parcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final ParcelService parcelService;

    @KafkaListener(topics = "consolidation-completed", groupId = "parcel-group")
    public void listenConsolidationCompleted(String message) {
        try {
            // Handle consolidation completed event
            log.info("Received consolidation completed event: {}", message);
            // Update parcel status or trigger other actions
        } catch (Exception e) {
            log.error("Error processing consolidation completed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "warehouse-update", groupId = "parcel-group")
    public void listenWarehouseUpdate(String message) {
        try {
            // Handle warehouse update event
            log.info("Received warehouse update event: {}", message);
            // Update parcel location or status
        } catch (Exception e) {
            log.error("Error processing warehouse update event: {}", e.getMessage());
        }
    }
}