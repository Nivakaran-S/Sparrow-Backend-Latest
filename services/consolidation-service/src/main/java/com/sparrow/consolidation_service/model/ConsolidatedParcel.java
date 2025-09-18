package com.sparrow.consolidation_service.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "consolidated_parcels")
public class ConsolidatedParcel {
    @Id
    private String id;

    @NotBlank(message = "Consolidation ID is required")
    private String consolidationId;

    @NotEmpty(message = "At least one parcel must be included")
    private List<String> parcelIds;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Total weight is required")
    @Positive(message = "Total weight must be positive")
    private BigDecimal totalWeight;

    @NotNull(message = "Total volume is required")
    @Positive(message = "Total volume must be positive")
    private BigDecimal totalVolume;

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    private ConsolidationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ConsolidationStatus {
        PENDING, PROCESSING, COMPLETED, SHIPPED
    }
}