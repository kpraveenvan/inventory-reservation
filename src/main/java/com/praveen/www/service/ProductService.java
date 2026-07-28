package com.praveen.www.service;

import com.praveen.www.domain.Product;
import com.praveen.www.dto.ProductStockResponse;
import com.praveen.www.dto.SeedProductRequest;
import com.praveen.www.exception.ProductNotFoundException;
import com.praveen.www.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Product catalog and stock query operations, including startup seeding.
 */
@Service
public class ProductService implements ApplicationRunner {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductStockResponse> listProducts() {
        return productRepository.findAll().stream()
                .map(ProductStockResponse::from)
                .toList();
    }

    public ProductStockResponse getProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        return ProductStockResponse.from(product);
    }

    public ProductStockResponse seedProduct(SeedProductRequest request) {
        Product product = new Product(
                request.getProductId(),
                request.getName(),
                request.getAvailableQuantity());
        return ProductStockResponse.from(productRepository.save(product));
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.findAll().isEmpty()) {
            productRepository.save(new Product("SKU-100", "Wireless Headphones", 50));
            productRepository.save(new Product("SKU-200", "USB-C Cable", 100));
        }
    }
}
