package com.praveen.www.dto;

import com.praveen.www.domain.Reservation;
import com.praveen.www.domain.ReservationStatus;

import java.time.Instant;

/**
 * API response representing a reservation.
 */
public class ReservationResponse {

    private final String id;
    private final String productId;
    private final int quantity;
    private final ReservationStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;

    public ReservationResponse(
            String id,
            String productId,
            int quantity,
            ReservationStatus status,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getProductId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getExpiresAt());
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
