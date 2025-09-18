package com.sparrow.parcel_service.repository;


import com.sparrow.parcel_service.model.Parcel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelRepository extends MongoRepository<Parcel, String> {
    Optional<Parcel> findByTrackingNumber(String trackingNumber);

    List<Parcel> findByConsolidationId(String consolidationId);

    List<Parcel> findBySenderId(String senderId);

    List<Parcel> findByRecipientId(String recipientId);

    List<Parcel> findByStatus(String status);

    @Query("{ 'trackingHistory.status': ?0 }")
    List<Parcel> findByTrackingStatus(String status);
}