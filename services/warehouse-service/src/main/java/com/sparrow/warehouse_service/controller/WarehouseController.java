package com.sparrow.warehouse_service.controller;


import com.sparrow.warehouse_service.model.Warehouse;
import com.sparrow.warehouse_service.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse Management", description = "APIs for managing warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @Operation(summary = "Create a new warehouse")
    public ResponseEntity<Warehouse> createWarehouse(@Valid @RequestBody Warehouse warehouse) {
        return ResponseEntity.ok(warehouseService.createWarehouse(warehouse));
    }

    @GetMapping
    @Operation(summary = "Get all warehouses")
    public ResponseEntity<List<Warehouse>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<Warehouse> getWarehouseById(@PathVariable String id) {
        Optional<Warehouse> warehouse = warehouseService.getWarehouseById(id);
        return warehouse.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{warehouseCode}")
    @Operation(summary = "Get warehouse by code")
    public ResponseEntity<Warehouse> getWarehouseByCode(@PathVariable String warehouseCode) {
        Optional<Warehouse> warehouse = warehouseService.getWarehouseByCode(warehouseCode);
        return warehouse.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get warehouses by status")
    public ResponseEntity<List<Warehouse>> getWarehousesByStatus(
            @PathVariable Warehouse.WarehouseStatus status) {
        return ResponseEntity.ok(warehouseService.getWarehousesByStatus(status));
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get warehouses by city")
    public ResponseEntity<List<Warehouse>> getWarehousesByCity(@PathVariable String city) {
        return ResponseEntity.ok(warehouseService.getWarehousesByCity(city));
    }

    @PatchMapping("/{id}/capacity")
    @Operation(summary = "Update warehouse capacity utilization")
    public ResponseEntity<Warehouse> updateWarehouseCapacity(
            @PathVariable String id,
            @RequestParam BigDecimal utilization) {
        return ResponseEntity.ok(warehouseService.updateWarehouseCapacity(id, utilization));
    }

    @PatchMapping("/{id}/status/{status}")
    @Operation(summary = "Update warehouse status")
    public ResponseEntity<Warehouse> updateWarehouseStatus(
            @PathVariable String id,
            @PathVariable Warehouse.WarehouseStatus status) {
        return ResponseEntity.ok(warehouseService.updateWarehouseStatus(id, status));
    }

    @GetMapping("/available")
    @Operation(summary = "Find available warehouses with capacity")
    public ResponseEntity<List<Warehouse>> findAvailableWarehouses(
            @RequestParam(required = false) BigDecimal requiredCapacity,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(warehouseService.findAvailableWarehouses(requiredCapacity, city));
    }
}