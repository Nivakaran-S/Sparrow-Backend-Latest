package com.sparrow.parcel_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparrow.parcel_service.model.Parcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendParcelCreatedEvent(Parcel parcel) {
        try {
            String message = objectMapper.writeValueAsString(parcel);
            kafkaTemplate.send("parcel-created", parcel.getId(), message);
            log.info("Sent parcel created event for parcel ID: {}", parcel.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing parcel object: {}", e.getMessage());
        }
    }

    public void sendParcelStatusUpdatedEvent(Parcel parcel) {
        try {
            String message = objectMapper.writeValueAsString(parcel);
            kafkaTemplate.send("parcel-status-updated", parcel.getId(), message);
            log.info("Sent parcel status updated event for parcel ID: {}", parcel.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing parcel object: {}", e.getMessage());
        }
    }

    public void sendParcelForConsolidation(Parcel parcel, String consolidationId) {
        try {
            String message = objectMapper.writeValueAsString(parcel);
            kafkaTemplate.send("consolidation-parcels", consolidationId, message);
            log.info("Sent parcel for consolidation ID: {}", consolidationId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing parcel object: {}", e.getMessage());
        }
    }
}