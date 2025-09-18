package com.sparrow.parcel_service.service;


import com.sparrow.parcel_service.dto.CreateParcelRequest;
import com.sparrow.parcel_service.dto.TrackingUpdateRequest;
import com.sparrow.parcel_service.model.Parcel;
import com.sparrow.parcel_service.repository.ParcelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParcelService {

    private final ParcelRepository parcelRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public Parcel createParcel(CreateParcelRequest request) {
        Parcel parcel = new Parcel();
        parcel.setTrackingNumber(generateTrackingNumber());
        parcel.setSenderId(request.getSenderId());
        parcel.setRecipientId(request.getRecipientId());
        parcel.setSenderAddress(request.getSenderAddress());
        parcel.setRecipientAddress(request.getRecipientAddress());
        parcel.setWeight(request.getWeight());
        parcel.setLength(request.getLength());
        parcel.setWidth(request.getWidth());
        parcel.setHeight(request.getHeight());
        parcel.setStatus("CREATED");
        parcel.setCreatedAt(LocalDateTime.now());
        parcel.setUpdatedAt(LocalDateTime.now());

        Parcel savedParcel = parcelRepository.save(parcel);
        kafkaProducerService.sendParcelCreatedEvent(savedParcel);

        return savedParcel;
    }

    public Parcel getParcelById(String id) {
        return parcelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcel not found with id: " + id));
    }

    public Parcel getParcelByTrackingNumber(String trackingNumber) {
        return parcelRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Parcel not found with tracking number: " + trackingNumber));
    }

    public List<Parcel> getParcelsBySender(String senderId) {
        return parcelRepository.findBySenderId(senderId);
    }

    public List<Parcel> getParcelsByRecipient(String recipientId) {
        return parcelRepository.findByRecipientId(recipientId);
    }

    public List<Parcel> getParcelsByConsolidation(String consolidationId) {
        return parcelRepository.findByConsolidationId(consolidationId);
    }

    @Transactional
    public Parcel updateParcelStatus(String parcelId, TrackingUpdateRequest request) {
        Parcel parcel = getParcelById(parcelId);

        Parcel.TrackingEvent trackingEvent = new Parcel.TrackingEvent();
        trackingEvent.setTimestamp(LocalDateTime.now());
        trackingEvent.setLocation(request.getLocation());
        trackingEvent.setStatus(request.getStatus());
        trackingEvent.setDescription(request.getDescription());

        parcel.getTrackingHistory().add(trackingEvent);
        parcel.setStatus(request.getStatus());
        parcel.setCurrentLocation(request.getLocation());
        parcel.setUpdatedAt(LocalDateTime.now());

        Parcel updatedParcel = parcelRepository.save(parcel);
        kafkaProducerService.sendParcelStatusUpdatedEvent(updatedParcel);

        return updatedParcel;
    }

    @Transactional
    public Parcel assignToConsolidation(String parcelId, String consolidationId) {
        Parcel parcel = getParcelById(parcelId);
        parcel.setConsolidationId(consolidationId);
        parcel.setStatus("AT_WAREHOUSE");
        parcel.setUpdatedAt(LocalDateTime.now());

        Parcel updatedParcel = parcelRepository.save(parcel);
        kafkaProducerService.sendParcelForConsolidation(updatedParcel, consolidationId);

        return updatedParcel;
    }

    private String generateTrackingNumber() {
        return "TRK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}