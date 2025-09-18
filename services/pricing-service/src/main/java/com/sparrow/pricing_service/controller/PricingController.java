package com.sparrow.pricing_service.controller;


import com.sparrow.pricing_service.model.PriceCategory;
import com.sparrow.pricing_service.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pricing")
@Tag(name = "Pricing API", description = "API for managing parcel pricing categories")
public class PricingController {

    @Autowired
    private PricingService pricingService;

    @GetMapping("/categories")
    @Operation(summary = "Get all pricing categories")
    public List<PriceCategory> getAllCategories() {
        return pricingService.getAllCategories();
    }

    @GetMapping("/categories/active")
    @Operation(summary = "Get active pricing categories")
    public List<PriceCategory> getActiveCategories() {
        return pricingService.getActiveCategories();
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Get pricing category by ID")
    public ResponseEntity<PriceCategory> getCategoryById(@PathVariable String id) {
        Optional<PriceCategory> category = pricingService.getCategoryById(id);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new pricing category")
    public PriceCategory createCategory(@Valid @RequestBody PriceCategory category) {
        return pricingService.createCategory(category);
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a pricing category")
    public ResponseEntity<PriceCategory> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody PriceCategory categoryDetails) {
        Optional<PriceCategory> updatedCategory = pricingService.updateCategory(id, categoryDetails);
        return updatedCategory.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete a pricing category")
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        boolean deleted = pricingService.deleteCategory(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate price for a parcel")
    public ResponseEntity<Double> calculatePrice(
            @RequestParam String categoryId,
            @RequestParam double weight,
            @RequestParam double volume) {
        try {
            double price = pricingService.calculatePrice(categoryId, weight, volume);
            return ResponseEntity.ok(price);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}