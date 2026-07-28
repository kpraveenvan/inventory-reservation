package com.praveen.www.dto;

import com.praveen.www.domain.Product;

/**
 * API response for product stock levels.
 */
public class ProductStockResponse {

    private final String productId;
    private final String name;
    private final int availableQuantity;

    public ProductStockResponse(String productId, String name, int availableQuantity) {
        this.productId = productId;
        this.name = name;
        this.availableQuantity = availableQuantity;
    }

    public static ProductStockResponse from(Product product) {
        return new ProductStockResponse(
                product.getProductId(),
                product.getName(),
                product.getAvailableQuantity());
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
}
