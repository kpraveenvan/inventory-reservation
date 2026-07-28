package com.praveen.www.repository;

import com.praveen.www.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryProductRepositoryTest {

    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    @DisplayName("Save stores a product that can be retrieved by id")
    void save_shouldPersistProduct_whenValidProductProvided() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 10);

        // Act
        repository.save(product);
        Optional<Product> found = repository.findById("SKU-1");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Widget", found.get().getName());
        assertEquals(10, found.get().getAvailableQuantity());
    }

    @Test
    @DisplayName("Find by id returns empty for unknown product")
    void findById_shouldReturnEmpty_whenProductMissing() {
        // Act
        Optional<Product> found = repository.findById("missing");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Find all returns every stored product")
    void findAll_shouldReturnAllProducts_whenMultipleSaved() {
        // Arrange
        repository.save(new Product("SKU-1", "A", 1));
        repository.save(new Product("SKU-2", "B", 2));

        // Act
        List<Product> all = repository.findAll();

        // Assert
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("Exists by id reflects whether a product was saved")
    void existsById_shouldReturnTrue_whenProductExists() {
        // Arrange
        repository.save(new Product("SKU-1", "Widget", 5));

        // Act & Assert
        assertTrue(repository.existsById("SKU-1"));
        assertFalse(repository.existsById("missing"));
    }

    @Test
    @DisplayName("Save overwrites an existing product with the same id")
    void save_shouldOverwriteExisting_whenSameIdSavedAgain() {
        // Arrange
        repository.save(new Product("SKU-1", "Old", 1));

        // Act
        repository.save(new Product("SKU-1", "New", 9));

        // Assert
        Product found = repository.findById("SKU-1").orElseThrow();
        assertEquals("New", found.getName());
        assertEquals(9, found.getAvailableQuantity());
    }
}
