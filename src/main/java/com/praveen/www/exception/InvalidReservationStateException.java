package com.praveen.www.exception;

/**
 * Thrown when a reservation operation is invalid for the current status.
 */
public class InvalidReservationStateException extends RuntimeException {

    public InvalidReservationStateException(String message) {
        super(message);
    }
}
