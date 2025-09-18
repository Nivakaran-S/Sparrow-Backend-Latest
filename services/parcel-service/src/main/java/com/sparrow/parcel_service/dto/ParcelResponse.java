package com.sparrow.parcel_service.dto;


import lombok.Data;
import com.sparrow.parcel_service.model.Parcel;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ParcelResponse {
    private String id;
    private String trackingNumber;
    private String senderId;
    private String recipientId;
    private Parcel.Address senderAddress;
    private Parcel.Address recipientAddress;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    private String status;
    private String currentLocation;
    private String consolidationId;
    private List<Parcel.TrackingEvent> trackingHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime estimatedDelivery;
}