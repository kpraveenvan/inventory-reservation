package com.praveen.www.repository;

import com.praveen.www.domain.Product;

import java.util.List;
import java.util.Optional;

/**
 * Persistence SPI for product inventory.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String productId);

    List<Product> findAll();

    boolean existsById(String productId);
}
