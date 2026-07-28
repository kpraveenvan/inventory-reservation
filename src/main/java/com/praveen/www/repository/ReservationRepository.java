package com.praveen.www.repository;

import com.praveen.www.domain.Reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence SPI for inventory reservations.
 */
public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(String reservationId);

    List<Reservation> findActiveExpiredBefore(Instant cutoff);
}
