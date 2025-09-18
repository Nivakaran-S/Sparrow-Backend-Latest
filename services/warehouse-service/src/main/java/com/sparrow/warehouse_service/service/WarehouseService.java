package com.sparrow.warehouse_service.service;

import com.sparrow.warehouse_service.model.Warehouse;
import com.sparrow.warehouse_service.model.WarehouseCapacityEvent;
import com.sparrow.warehouse_service.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map; // Add this import

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Warehouse createWarehouse(Warehouse warehouse) {
        warehouse.setCreatedAt(LocalDateTime.now());
        warehouse.setUpdatedAt(LocalDateTime.now());
        warehouse.setStatus(Warehouse.WarehouseStatus.ACTIVE);

        if (warehouse.getCurrentUtilization() == null) {
            warehouse.setCurrentUtilization(BigDecimal.ZERO);
        }

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        sendWarehouseEvent(savedWarehouse, "CREATED");
        return savedWarehouse;
    }

    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    public Optional<Warehouse> getWarehouseById(String id) {
        return warehouseRepository.findById(id);
    }

    public Optional<Warehouse> getWarehouseByCode(String warehouseCode) {
        return warehouseRepository.findByWarehouseCode(warehouseCode);
    }

    public List<Warehouse> getWarehousesByStatus(Warehouse.WarehouseStatus status) {
        return warehouseRepository.findByStatus(status);
    }

    public List<Warehouse> getWarehousesByCity(String city) {
        return warehouseRepository.findByCity(city);
    }

    public Warehouse updateWarehouseCapacity(String id, BigDecimal newUtilization) {
        return warehouseRepository.findById(id).map(warehouse -> {
            WarehouseCapacityEvent event = createCapacityEvent(warehouse, newUtilization);

            warehouse.setCurrentUtilization(newUtilization);
            warehouse.setUpdatedAt(LocalDateTime.now());

            // Update status based on utilization
            if (newUtilization.compareTo(warehouse.getCapacity()) >= 0) {
                warehouse.setStatus(Warehouse.WarehouseStatus.FULL);
            } else if (warehouse.getStatus() == Warehouse.WarehouseStatus.FULL) {
                warehouse.setStatus(Warehouse.WarehouseStatus.ACTIVE);
            }

            Warehouse updated = warehouseRepository.save(warehouse);
            sendCapacityEvent(event);
            return updated;
        }).orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + id));
    }

    public Warehouse updateWarehouseStatus(String id, Warehouse.WarehouseStatus status) {
        return warehouseRepository.findById(id).map(warehouse -> {
            String previousStatus = warehouse.getStatus().name();
            warehouse.setStatus(status);
            warehouse.setUpdatedAt(LocalDateTime.now());

            Warehouse updated = warehouseRepository.save(warehouse);
            sendStatusChangeEvent(warehouse, previousStatus, status.name());
            return updated;
        }).orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + id));
    }

    public List<Warehouse> findAvailableWarehouses(BigDecimal requiredCapacity, String city) {
        BigDecimal utilizationThreshold = new BigDecimal("0.8"); // 80% utilization threshold

        if (city != null) {
            // Use the correct repository method
            return warehouseRepository.findByCityAndCurrentUtilizationLessThan(city, utilizationThreshold);
        }
        // Use the correct repository method
        return warehouseRepository.findByCurrentUtilizationLessThan(utilizationThreshold);
    }

    private WarehouseCapacityEvent createCapacityEvent(Warehouse warehouse, BigDecimal newUtilization) {
        WarehouseCapacityEvent event = new WarehouseCapacityEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWarehouseId(warehouse.getId());
        event.setWarehouseCode(warehouse.getWarehouseCode());
        event.setEventType(WarehouseCapacityEvent.EventType.CAPACITY_UPDATE.name());
        event.setPreviousCapacity(warehouse.getCapacity());
        event.setNewCapacity(warehouse.getCapacity());
        event.setPreviousUtilization(warehouse.getCurrentUtilization());
        event.setNewUtilization(newUtilization);
        event.setEventTimestamp(LocalDateTime.now());
        return event;
    }

    private void sendCapacityEvent(WarehouseCapacityEvent event) {
        kafkaTemplate.send("warehouse-capacity-events", event.getWarehouseId(), event);
    }

    private void sendStatusChangeEvent(Warehouse warehouse, String previousStatus, String newStatus) {
        WarehouseCapacityEvent event = new WarehouseCapacityEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWarehouseId(warehouse.getId());
        event.setWarehouseCode(warehouse.getWarehouseCode());
        event.setEventType(WarehouseCapacityEvent.EventType.STATUS_CHANGE.name());
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setEventTimestamp(LocalDateTime.now());
        kafkaTemplate.send("warehouse-status-events", warehouse.getId(), event);
    }

    private void sendWarehouseEvent(Warehouse warehouse, String action) {
        // Use HashMap instead of Map.of for Java 11+ compatibility
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("action", action);
        eventData.put("warehouse", warehouse);
        eventData.put("timestamp", LocalDateTime.now());

        kafkaTemplate.send("warehouse-events", warehouse.getId(), eventData);
    }
}