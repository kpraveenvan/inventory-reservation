package com.praveen.www.exception;

/**
 * Thrown when a reservation cannot be found by id.
 */
public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
