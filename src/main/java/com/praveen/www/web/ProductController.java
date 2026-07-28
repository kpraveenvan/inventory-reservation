package com.praveen.www.web;

import com.praveen.www.dto.ProductStockResponse;
import com.praveen.www.dto.SeedProductRequest;
import com.praveen.www.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for product stock listing and seeding.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductStockResponse> listProducts() {
        return productService.listProducts();
    }

    @GetMapping("/{productId}")
    public ProductStockResponse getProduct(@PathVariable String productId) {
        return productService.getProduct(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductStockResponse seedProduct(@Valid @RequestBody SeedProductRequest request) {
        return productService.seedProduct(request);
    }
}
