package com.sparrow.pricing_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;

@Document(collection = "price_categories")
public class PriceCategory {
    @Id
    private String id;

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be positive")
    private Double basePrice;

    @NotNull(message = "Price per kg is required")
    @Positive(message = "Price per kg must be positive")
    private Double pricePerKg;

    @NotNull(message = "Price per cubic cm is required")
    @Positive(message = "Price per cubic cm must be positive")
    private Double pricePerCubicCm;

    private boolean active = true;

    public PriceCategory() {}

    public PriceCategory(String name, String description, Double basePrice,
                         Double pricePerKg, Double pricePerCubicCm) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.pricePerKg = pricePerKg;
        this.pricePerCubicCm = pricePerCubicCm;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }

    public Double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(Double pricePerKg) { this.pricePerKg = pricePerKg; }

    public Double getPricePerCubicCm() { return pricePerCubicCm; }
    public void setPricePerCubicCm(Double pricePerCubicCm) { this.pricePerCubicCm = pricePerCubicCm; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}