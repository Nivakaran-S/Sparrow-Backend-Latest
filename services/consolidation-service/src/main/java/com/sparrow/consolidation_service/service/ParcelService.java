package com.sparrow.consolidation_service.service;


import com.sparrow.consolidation_service.model.Parcel;
import com.sparrow.consolidation_service.repository.ParcelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParcelService {

    private final ParcelRepository parcelRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Parcel createParcel(Parcel parcel) {
        parcel.setCreatedAt(LocalDateTime.now());
        parcel.setUpdatedAt(LocalDateTime.now());
        parcel.setStatus(Parcel.ParcelStatus.RECEIVED);

        Parcel savedParcel = parcelRepository.save(parcel);

        // Send Kafka event
        kafkaTemplate.send("parcel-created", savedParcel.getId(), savedParcel);

        return savedParcel;
    }

    public List<Parcel> getAllParcels() {
        return parcelRepository.findAll();
    }

    public Optional<Parcel> getParcelById(String id) {
        return parcelRepository.findById(id);
    }

    public List<Parcel> getParcelsByCustomerId(String customerId) {
        return parcelRepository.findByCustomerId(customerId);
    }

    public List<Parcel> getParcelsByStatus(Parcel.ParcelStatus status) {
        return parcelRepository.findByStatus(status);
    }

    public Parcel updateParcelStatus(String id, Parcel.ParcelStatus status) {
        return parcelRepository.findById(id).map(parcel -> {
            parcel.setStatus(status);
            parcel.setUpdatedAt(LocalDateTime.now());
            return parcelRepository.save(parcel);
        }).orElseThrow(() -> new RuntimeException("Parcel not found with id: " + id));
    }

    public void deleteParcel(String id) {
        parcelRepository.deleteById(id);
    }
}
