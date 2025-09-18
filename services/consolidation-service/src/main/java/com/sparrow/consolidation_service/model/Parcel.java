package com.sparrow.consolidation_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "parcels")
public class Parcel {
    @Id
    private String id;

    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private BigDecimal weight;

    @NotNull(message = "Volume is required")
    @Positive(message = "Volume must be positive")
    private BigDecimal volume;

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    private ParcelStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ParcelStatus {
        RECEIVED, PROCESSING, CONSOLIDATED, SHIPPED, DELIVERED
    }
}