package com.sparrow.consolidation_service.consumer;

import com.sparrow.consolidation_service.model.Parcel;
import com.sparrow.consolidation_service.service.ParcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParcelConsumer {

    private final ParcelService parcelService;

    @KafkaListener(topics = "parcel-updates", groupId = "parcel-consolidation-group")
    public void consumeParcelUpdate(Parcel parcel) {
        System.out.println("Received parcel update: " + parcel.getTrackingNumber());
        // Process the update as needed
    }
}