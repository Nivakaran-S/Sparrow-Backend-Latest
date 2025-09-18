package com.sparrow.consolidation_service.controller;

import com.sparrow.consolidation_service.model.Parcel;
import com.sparrow.consolidation_service.service.ParcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
@Tag(name = "Parcel Management", description = "APIs for managing parcels")
public class ParcelController {

    private final ParcelService parcelService;

    @PostMapping
    @Operation(summary = "Create a new parcel")
    public ResponseEntity<Parcel> createParcel(@Valid @RequestBody Parcel parcel) {
        return ResponseEntity.ok(parcelService.createParcel(parcel));
    }

    @GetMapping
    @Operation(summary = "Get all parcels")
    public ResponseEntity<List<Parcel>> getAllParcels() {
        return ResponseEntity.ok(parcelService.getAllParcels());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get parcel by ID")
    public ResponseEntity<Parcel> getParcelById(@PathVariable String id) {
        return parcelService.getParcelById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get parcels by customer ID")
    public ResponseEntity<List<Parcel>> getParcelsByCustomerId(@PathVariable String customerId) {
        return ResponseEntity.ok(parcelService.getParcelsByCustomerId(customerId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get parcels by status")
    public ResponseEntity<List<Parcel>> getParcelsByStatus(@PathVariable Parcel.ParcelStatus status) {
        return ResponseEntity.ok(parcelService.getParcelsByStatus(status));
    }

    @PatchMapping("/{id}/status/{status}")
    @Operation(summary = "Update parcel status")
    public ResponseEntity<Parcel> updateParcelStatus(
            @PathVariable String id,
            @PathVariable Parcel.ParcelStatus status) {
        return ResponseEntity.ok(parcelService.updateParcelStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete parcel")
    public ResponseEntity<Void> deleteParcel(@PathVariable String id) {
        parcelService.deleteParcel(id);
        return ResponseEntity.noContent().build();
    }
}