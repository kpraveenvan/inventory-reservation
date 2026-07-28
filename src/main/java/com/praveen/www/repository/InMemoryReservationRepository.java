package com.praveen.www.repository;

import com.praveen.www.domain.Reservation;
import com.praveen.www.domain.ReservationStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ConcurrentHashMap-backed reservation repository.
 */
@Repository
public class InMemoryReservationRepository implements ReservationRepository {

    private final ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<>();

    @Override
    public Reservation save(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return Optional.ofNullable(reservations.get(reservationId));
    }

    @Override
    public List<Reservation> findActiveExpiredBefore(Instant cutoff) {
        List<Reservation> expired = new ArrayList<>();
        for (Reservation reservation : reservations.values()) {
            if (reservation.getStatus() == ReservationStatus.ACTIVE
                    && !reservation.getExpiresAt().isAfter(cutoff)) {
                expired.add(reservation);
            }
        }
        return expired;
    }
}
