package com.sparrow.parcel_service.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "parcels")
public class Parcel {
    @Id
    private String id;

    @Indexed
    private String trackingNumber;

    private String senderId;
    private String recipientId;
    private Address senderAddress;
    private Address recipientAddress;

    private Double weight;
    private Double length;
    private Double width;
    private Double height;

    private String status; // CREATED, IN_TRANSIT, AT_WAREHOUSE, OUT_FOR_DELIVERY, DELIVERED
    private String currentLocation;

    @Indexed
    private String consolidationId;

    private List<TrackingEvent> trackingHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime estimatedDelivery;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingEvent {
        private LocalDateTime timestamp;
        private String location;
        private String status;
        private String description;
    }
}