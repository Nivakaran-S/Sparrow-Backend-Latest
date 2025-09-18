package com.sparrow.pricing_service.repository;


import com.sparrow.pricing_service.model.PriceCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PriceCategoryRepository extends MongoRepository<PriceCategory, String> {
    List<PriceCategory> findByActive(boolean active);
    Optional<PriceCategory> findByName(String name);
    boolean existsByName(String name);
}