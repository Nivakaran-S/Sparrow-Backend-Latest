package com.sparrow.warehouse_service.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "warehouses")
public class Warehouse {
    @Id
    private String id;

    @NotBlank(message = "Warehouse code is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Warehouse code must contain only uppercase letters, numbers, hyphens, or underscores")
    private String warehouseCode;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    private BigDecimal capacity; // in cubic meters

    @NotNull(message = "Current utilization is required")
    @DecimalMin(value = "0.0", message = "Current utilization cannot be negative")
    private BigDecimal currentUtilization;

    private List<String> supportedParcelTypes;
    private List<String> availableServices;

    private WarehouseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class Location {
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    private Location location;

    public enum WarehouseStatus {
        ACTIVE, INACTIVE, MAINTENANCE, FULL
    }
}
