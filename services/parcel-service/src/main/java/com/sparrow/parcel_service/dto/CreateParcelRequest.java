package com.sparrow.parcel_service.dto;


import com.sparrow.parcel_service.model.Parcel;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class CreateParcelRequest {
    @NotBlank
    private String senderId;

    @NotBlank
    private String recipientId;

    @NotNull
    private Parcel.Address senderAddress;

    @NotNull
    private Parcel.Address recipientAddress;

    @Positive
    private Double weight;

    @Positive
    private Double length;

    @Positive
    private Double width;

    @Positive
    private Double height;
}