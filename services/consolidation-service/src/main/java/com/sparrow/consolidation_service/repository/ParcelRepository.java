package com.sparrow.consolidation_service.repository;


import com.sparrow.consolidation_service.model.Parcel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelRepository extends MongoRepository<Parcel, String> {
    List<Parcel> findByCustomerId(String customerId);
    List<Parcel> findByStatus(Parcel.ParcelStatus status);
    Optional<Parcel> findByTrackingNumber(String trackingNumber);
    List<Parcel> findByCustomerIdAndStatus(String customerId, Parcel.ParcelStatus status);
}