package com.sparrow.warehouse_service.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WarehouseCapacityEvent {
    private String eventId;
    private String warehouseId;
    private String warehouseCode;
    private String eventType; // CAPACITY_UPDATE, STATUS_CHANGE, etc.
    private BigDecimal previousCapacity;
    private BigDecimal newCapacity;
    private BigDecimal previousUtilization;
    private BigDecimal newUtilization;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime eventTimestamp;

    public enum EventType {
        CAPACITY_UPDATE, STATUS_CHANGE, PARCEL_RECEIVED, PARCEL_SHIPPED
    }
}