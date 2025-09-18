package com.sparrow.parcel_service.dto;


import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class TrackingUpdateRequest {
    @NotBlank
    private String location;

    @NotBlank
    private String status;

    private String description;
}