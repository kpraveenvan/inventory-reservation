package com.praveen.www.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for seeding a product with initial stock.
 */
public class SeedProductRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "availableQuantity is required")
    @Min(value = 0, message = "availableQuantity must not be negative")
    private Integer availableQuantity;

    public SeedProductRequest() {
    }

    public SeedProductRequest(String productId, String name, Integer availableQuantity) {
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
