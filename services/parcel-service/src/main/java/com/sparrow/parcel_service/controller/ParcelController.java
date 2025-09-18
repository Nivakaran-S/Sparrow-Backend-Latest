package com.sparrow.parcel_service.controller;


import com.sparrow.parcel_service.dto.CreateParcelRequest;
import com.sparrow.parcel_service.dto.ParcelResponse;
import com.sparrow.parcel_service.dto.TrackingUpdateRequest;
import com.sparrow.parcel_service.model.Parcel;
import com.sparrow.parcel_service.service.ParcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
@Tag(name = "Parcel Management", description = "APIs for managing parcels and tracking")
public class ParcelController {

    private final ParcelService parcelService;

    @PostMapping
    @Operation(summary = "Create a new parcel", description = "Creates a new parcel with tracking information")
    public ResponseEntity<ParcelResponse> createParcel(@Valid @RequestBody CreateParcelRequest request) {
        Parcel parcel = parcelService.createParcel(request);
        return ResponseEntity.ok(convertToResponse(parcel));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get parcel by ID", description = "Retrieves parcel details by its ID")
    public ResponseEntity<ParcelResponse> getParcelById(@PathVariable String id) {
        Parcel parcel = parcelService.getParcelById(id);
        return ResponseEntity.ok(convertToResponse(parcel));
    }

    @GetMapping("/tracking/{trackingNumber}")
    @Operation(summary = "Get parcel by tracking number", description = "Retrieves parcel details by tracking number")
    public ResponseEntity<ParcelResponse> getParcelByTrackingNumber(@PathVariable String trackingNumber) {
        Parcel parcel = parcelService.getParcelByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(convertToResponse(parcel));
    }

    @GetMapping("/sender/{senderId}")
    @Operation(summary = "Get parcels by sender", description = "Retrieves all parcels sent by a specific sender")
    public ResponseEntity<List<ParcelResponse>> getParcelsBySender(@PathVariable String senderId) {
        List<Parcel> parcels = parcelService.getParcelsBySender(senderId);
        return ResponseEntity.ok(parcels.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Get parcels by recipient", description = "Retrieves all parcels for a specific recipient")
    public ResponseEntity<List<ParcelResponse>> getParcelsByRecipient(@PathVariable String recipientId) {
        List<Parcel> parcels = parcelService.getParcelsByRecipient(recipientId);
        return ResponseEntity.ok(parcels.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/consolidation/{consolidationId}")
    @Operation(summary = "Get parcels by consolidation", description = "Retrieves all parcels in a specific consolidation")
    public ResponseEntity<List<ParcelResponse>> getParcelsByConsolidation(@PathVariable String consolidationId) {
        List<Parcel> parcels = parcelService.getParcelsByConsolidation(consolidationId);
        return ResponseEntity.ok(parcels.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update parcel status", description = "Updates the status and tracking information of a parcel")
    public ResponseEntity<ParcelResponse> updateParcelStatus(
            @PathVariable String id,
            @Valid @RequestBody TrackingUpdateRequest request) {
        Parcel parcel = parcelService.updateParcelStatus(id, request);
        return ResponseEntity.ok(convertToResponse(parcel));
    }

    @PatchMapping("/{id}/consolidation/{consolidationId}")
    @Operation(summary = "Assign parcel to consolidation", description = "Assigns a parcel to a consolidation batch")
    public ResponseEntity<ParcelResponse> assignToConsolidation(
            @PathVariable String id,
            @PathVariable String consolidationId) {
        Parcel parcel = parcelService.assignToConsolidation(id, consolidationId);
        return ResponseEntity.ok(convertToResponse(parcel));
    }

    private ParcelResponse convertToResponse(Parcel parcel) {
        ParcelResponse response = new ParcelResponse();
        response.setId(parcel.getId());
        response.setTrackingNumber(parcel.getTrackingNumber());
        response.setSenderId(parcel.getSenderId());
        response.setRecipientId(parcel.getRecipientId());
        response.setSenderAddress(parcel.getSenderAddress());
        response.setRecipientAddress(parcel.getRecipientAddress());
        response.setWeight(parcel.getWeight());
        response.setLength(parcel.getLength());
        response.setWidth(parcel.getWidth());
        response.setHeight(parcel.getHeight());
        response.setStatus(parcel.getStatus());
        response.setCurrentLocation(parcel.getCurrentLocation());
        response.setConsolidationId(parcel.getConsolidationId());
        response.setTrackingHistory(parcel.getTrackingHistory());
        response.setCreatedAt(parcel.getCreatedAt());
        response.setUpdatedAt(parcel.getUpdatedAt());
        response.setEstimatedDelivery(parcel.getEstimatedDelivery());
        return response;
    }
}