package com.praveen.www.repository;

import com.praveen.www.domain.Product;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ConcurrentHashMap-backed product repository.
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentHashMap<String, Product> products = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        products.put(product.getProductId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    @Override
    public boolean existsById(String productId) {
        return products.containsKey(productId);
    }
}
