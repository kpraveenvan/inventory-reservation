package com.praveen.www.domain;

/**
 * Inventory product with available stock that can be reserved.
 */
public class Product {

    private final String productId;
    private final String name;
    private int availableQuantity;

    public Product(String productId, String name, int availableQuantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must not be negative");
        }
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must not be negative");
        }
        this.availableQuantity = availableQuantity;
    }
}
