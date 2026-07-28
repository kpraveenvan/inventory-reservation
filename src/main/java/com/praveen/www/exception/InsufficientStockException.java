package com.praveen.www.exception;

/**
 * Thrown when requested reservation quantity exceeds available stock.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
