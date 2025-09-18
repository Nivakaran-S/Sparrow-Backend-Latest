package com.sparrow.pricing_service.service;


import com.sparrow.pricing_service.model.PriceCategory;
import com.sparrow.pricing_service.repository.PriceCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PricingService {

    @Autowired
    private PriceCategoryRepository repository;

    public List<PriceCategory> getAllCategories() {
        return repository.findAll();
    }

    public List<PriceCategory> getActiveCategories() {
        return repository.findByActive(true);
    }

    public Optional<PriceCategory> getCategoryById(String id) {
        return repository.findById(id);
    }

    public PriceCategory createCategory(PriceCategory category) {
        return repository.save(category);
    }

    public Optional<PriceCategory> updateCategory(String id, PriceCategory categoryDetails) {
        return repository.findById(id).map(existingCategory -> {
            existingCategory.setName(categoryDetails.getName());
            existingCategory.setDescription(categoryDetails.getDescription());
            existingCategory.setBasePrice(categoryDetails.getBasePrice());
            existingCategory.setPricePerKg(categoryDetails.getPricePerKg());
            existingCategory.setPricePerCubicCm(categoryDetails.getPricePerCubicCm());
            existingCategory.setActive(categoryDetails.isActive());
            return repository.save(existingCategory);
        });
    }

    public boolean deleteCategory(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    public double calculatePrice(String categoryId, double weight, double volume) {
        return repository.findById(categoryId).map(category -> {
            return category.getBasePrice() +
                    (category.getPricePerKg() * weight) +
                    (category.getPricePerCubicCm() * volume);
        }).orElseThrow(() -> new RuntimeException("Category not found"));
    }
}