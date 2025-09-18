package com.sparrow.consolidation_service.controller;

import com.sparrow.consolidation_service.model.ConsolidatedParcel;
import com.sparrow.consolidation_service.service.ConsolidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consolidations")
@RequiredArgsConstructor
@Tag(name = "Consolidation Management", description = "APIs for managing parcel consolidations")
public class ConsolidationController {

    private final ConsolidationService consolidationService;

    @PostMapping
    @Operation(summary = "Create a new consolidation")
    public ResponseEntity<ConsolidatedParcel> createConsolidation(
            @RequestParam String customerId,
            @RequestBody List<String> parcelIds) {
        return ResponseEntity.ok(consolidationService.createConsolidation(customerId, parcelIds));
    }

    @GetMapping
    @Operation(summary = "Get all consolidations")
    public ResponseEntity<List<ConsolidatedParcel>> getAllConsolidations() {
        return ResponseEntity.ok(consolidationService.getAllConsolidations());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get consolidations by customer ID")
    public ResponseEntity<List<ConsolidatedParcel>> getConsolidationsByCustomerId(
            @PathVariable String customerId) {
        return ResponseEntity.ok(consolidationService.getConsolidationsByCustomerId(customerId));
    }

    @PatchMapping("/{id}/status/{status}")
    @Operation(summary = "Update consolidation status")
    public ResponseEntity<ConsolidatedParcel> updateConsolidationStatus(
            @PathVariable String id,
            @PathVariable ConsolidatedParcel.ConsolidationStatus status) {
        return ResponseEntity.ok(consolidationService.updateConsolidationStatus(id, status));
    }
}