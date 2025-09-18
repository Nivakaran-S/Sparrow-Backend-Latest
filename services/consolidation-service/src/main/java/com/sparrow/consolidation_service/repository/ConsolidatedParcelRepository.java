package com.sparrow.consolidation_service.repository;


import com.sparrow.consolidation_service.model.ConsolidatedParcel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsolidatedParcelRepository extends MongoRepository<ConsolidatedParcel, String> {
    List<ConsolidatedParcel> findByCustomerId(String customerId);
    List<ConsolidatedParcel> findByStatus(ConsolidatedParcel.ConsolidationStatus status);
    Optional<ConsolidatedParcel> findByConsolidationId(String consolidationId);
}