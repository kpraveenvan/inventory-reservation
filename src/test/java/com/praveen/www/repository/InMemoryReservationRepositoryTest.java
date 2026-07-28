package com.praveen.www.repository;

import com.praveen.www.domain.Reservation;
import com.praveen.www.domain.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryReservationRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    private InMemoryReservationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReservationRepository();
    }

    @Test
    @DisplayName("Save stores a reservation that can be retrieved by id")
    void save_shouldPersistReservation_whenValidReservationProvided() {
        // Arrange
        Reservation reservation = new Reservation(
                "res-1", "SKU-1", 2, ReservationStatus.ACTIVE, NOW, NOW.plusSeconds(60));

        // Act
        repository.save(reservation);
        Optional<Reservation> found = repository.findById("res-1");

        // Assert
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getQuantity());
        assertEquals(ReservationStatus.ACTIVE, found.get().getStatus());
    }

    @Test
    @DisplayName("Find by id returns empty for unknown reservation")
    void findById_shouldReturnEmpty_whenReservationMissing() {
        // Act
        Optional<Reservation> found = repository.findById("missing");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Find active expired returns only ACTIVE reservations past cutoff")
    void findActiveExpiredBefore_shouldReturnOnlyExpiredActive_whenMixedStatusesPresent() {
        // Arrange
        Reservation expiredActive = new Reservation(
                "res-1", "SKU-1", 1, ReservationStatus.ACTIVE, NOW, NOW.minusSeconds(10));
        Reservation futureActive = new Reservation(
                "res-2", "SKU-1", 1, ReservationStatus.ACTIVE, NOW, NOW.plusSeconds(60));
        Reservation expiredConfirmed = new Reservation(
                "res-3", "SKU-1", 1, ReservationStatus.CONFIRMED, NOW, NOW.minusSeconds(10));
        repository.save(expiredActive);
        repository.save(futureActive);
        repository.save(expiredConfirmed);

        // Act
        List<Reservation> result = repository.findActiveExpiredBefore(NOW);

        // Assert
        assertEquals(1, result.size());
        assertEquals("res-1", result.get(0).getId());
    }
}
