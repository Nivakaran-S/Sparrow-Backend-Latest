package com.sparrow.warehouse_service.consumer;


import com.sparrow.warehouse_service.model.Warehouse;
import com.sparrow.warehouse_service.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParcelEventListener {

    private final WarehouseService warehouseService;

    @KafkaListener(topics = "parcel-created", groupId = "warehouse-service-group")
    public void handleParcelCreated(Map<String, Object> parcelEvent) {
        log.info("Received parcel created event: {}", parcelEvent);
        // Logic to assign warehouse to parcel based on location and capacity
    }

    @KafkaListener(topics = "parcel-consolidated", groupId = "warehouse-service-group")
    public void handleParcelConsolidated(Map<String, Object> consolidationEvent) {
        log.info("Received parcel consolidated event: {}", consolidationEvent);
        // Update warehouse capacity when parcels are consolidated
    }
}
