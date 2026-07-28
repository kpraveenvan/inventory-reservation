package com.praveen.www.exception;

/**
 * Thrown when a product cannot be found by id.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}
