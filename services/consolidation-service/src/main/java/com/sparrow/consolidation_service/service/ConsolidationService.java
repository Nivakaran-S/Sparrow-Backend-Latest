package com.sparrow.consolidation_service.service;


import com.sparrow.consolidation_service.model.ConsolidatedParcel;
import com.sparrow.consolidation_service.model.Parcel;
import com.sparrow.consolidation_service.repository.ConsolidatedParcelRepository;
import com.sparrow.consolidation_service.repository.ParcelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsolidationService {

    private final ConsolidatedParcelRepository consolidatedParcelRepository;
    private final ParcelRepository parcelRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ConsolidatedParcel createConsolidation(String customerId, List<String> parcelIds) {
        List<Parcel> parcels = parcelRepository.findAllById(parcelIds);

        if (parcels.isEmpty()) {
            throw new RuntimeException("No parcels found for the provided IDs");
        }

        // Calculate totals
        BigDecimal totalWeight = parcels.stream()
                .map(Parcel::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVolume = parcels.stream()
                .map(Parcel::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ConsolidatedParcel consolidation = new ConsolidatedParcel();
        consolidation.setConsolidationId(UUID.randomUUID().toString());
        consolidation.setParcelIds(parcelIds);
        consolidation.setCustomerId(customerId);
        consolidation.setTotalWeight(totalWeight);
        consolidation.setTotalVolume(totalVolume);
        consolidation.setOrigin(parcels.get(0).getOrigin());
        consolidation.setDestination(parcels.get(0).getDestination());
        consolidation.setStatus(ConsolidatedParcel.ConsolidationStatus.PENDING);
        consolidation.setCreatedAt(LocalDateTime.now());
        consolidation.setUpdatedAt(LocalDateTime.now());

        ConsolidatedParcel savedConsolidation = consolidatedParcelRepository.save(consolidation);

        // Update parcel statuses
        parcels.forEach(parcel -> {
            parcel.setStatus(Parcel.ParcelStatus.CONSOLIDATED);
            parcel.setUpdatedAt(LocalDateTime.now());
            parcelRepository.save(parcel);
        });

        // Send Kafka event
        kafkaTemplate.send("parcel-consolidated", savedConsolidation.getId(), savedConsolidation);

        return savedConsolidation;
    }

    public List<ConsolidatedParcel> getAllConsolidations() {
        return consolidatedParcelRepository.findAll();
    }

    public ConsolidatedParcel updateConsolidationStatus(String id, ConsolidatedParcel.ConsolidationStatus status) {
        return consolidatedParcelRepository.findById(id).map(consolidation -> {
            consolidation.setStatus(status);
            consolidation.setUpdatedAt(LocalDateTime.now());

            ConsolidatedParcel updated = consolidatedParcelRepository.save(consolidation);

            // Send status update event
            kafkaTemplate.send("consolidation-status", updated.getId(), updated);

            return updated;
        }).orElseThrow(() -> new RuntimeException("Consolidation not found with id: " + id));
    }

    public List<ConsolidatedParcel> getConsolidationsByCustomerId(String customerId) {
        return consolidatedParcelRepository.findByCustomerId(customerId);
    }
}
