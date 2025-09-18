package com.sparrow.warehouse_service.repository;

import com.sparrow.warehouse_service.model.Warehouse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends MongoRepository<Warehouse, String> {
    Optional<Warehouse> findByWarehouseCode(String warehouseCode);
    List<Warehouse> findByStatus(Warehouse.WarehouseStatus status);
    List<Warehouse> findByCity(String city);
    List<Warehouse> findByCountry(String country);

    // Fixed method names
    @Query("{ 'currentUtilization': { $lt: ?0 } }")
    List<Warehouse> findByCurrentUtilizationLessThan(BigDecimal utilizationThreshold);

    @Query("{ 'currentUtilization': { $gt: ?0 } }")
    List<Warehouse> findByCurrentUtilizationGreaterThan(BigDecimal utilizationThreshold);

    // Add this method for city and utilization query
    @Query("{ 'city': ?0, 'currentUtilization': { $lt: ?1 } }")
    List<Warehouse> findByCityAndCurrentUtilizationLessThan(String city, BigDecimal utilizationThreshold);

    List<Warehouse> findBySupportedParcelTypesContaining(String parcelType);
    List<Warehouse> findByAvailableServicesContaining(String service);
}