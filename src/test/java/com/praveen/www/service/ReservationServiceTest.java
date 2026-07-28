package com.praveen.www.service;

import com.praveen.www.config.ReservationProperties;
import com.praveen.www.domain.Product;
import com.praveen.www.domain.Reservation;
import com.praveen.www.domain.ReservationStatus;
import com.praveen.www.dto.ReservationResponse;
import com.praveen.www.dto.ReserveRequest;
import com.praveen.www.exception.InsufficientStockException;
import com.praveen.www.exception.InvalidReservationStateException;
import com.praveen.www.exception.ProductNotFoundException;
import com.praveen.www.exception.ReservationNotFoundException;
import com.praveen.www.repository.ProductRepository;
import com.praveen.www.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReservationRepository reservationRepository;

    private ReservationProperties properties;
    private Clock clock;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        properties = new ReservationProperties();
        properties.setTtlSeconds(600);
        properties.setExpirySweepMs(5000);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new ReservationService(productRepository, reservationRepository, properties, clock);
    }

    @Test
    @DisplayName("Reserve decreases available stock and creates an ACTIVE reservation")
    void reserve_shouldCreateActiveReservation_whenStockIsSufficient() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 10);
        when(productRepository.findById("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReservationResponse response = service.reserve(new ReserveRequest("SKU-1", 3));

        // Assert
        assertNotNull(response.getId());
        assertEquals("SKU-1", response.getProductId());
        assertEquals(3, response.getQuantity());
        assertEquals(ReservationStatus.ACTIVE, response.getStatus());
        assertEquals(NOW, response.getCreatedAt());
        assertEquals(NOW.plusSeconds(600), response.getExpiresAt());
        assertEquals(7, product.getAvailableQuantity());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Reserve fails with insufficient stock when quantity exceeds available")
    void reserve_shouldThrowInsufficientStock_whenRequestedExceedsAvailable() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 2);
        when(productRepository.findById("SKU-1")).thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(
                InsufficientStockException.class,
                () -> service.reserve(new ReserveRequest("SKU-1", 5)));
        assertEquals(2, product.getAvailableQuantity());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reserve fails when product does not exist")
    void reserve_shouldThrowProductNotFound_whenProductMissing() {
        // Arrange
        when(productRepository.findById("MISSING")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ProductNotFoundException.class,
                () -> service.reserve(new ReserveRequest("MISSING", 1)));
    }

    @Test
    @DisplayName("Confirm marks an ACTIVE reservation as CONFIRMED without returning stock")
    void confirm_shouldMarkConfirmed_whenReservationIsActive() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 7);
        Reservation reservation = activeReservation("res-1", "SKU-1", 3, NOW.plusSeconds(600));
        when(reservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReservationResponse response = service.confirm("res-1");

        // Assert
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        assertEquals(7, product.getAvailableQuantity());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Confirm rejects a reservation that is not ACTIVE")
    void confirm_shouldThrowInvalidState_whenAlreadyConfirmed() {
        // Arrange
        Reservation reservation = new Reservation(
                "res-1", "SKU-1", 3, ReservationStatus.CONFIRMED, NOW, NOW.plusSeconds(600));
        when(reservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThrows(InvalidReservationStateException.class, () -> service.confirm("res-1"));
    }

    @Test
    @DisplayName("Confirm rejects an expired reservation and releases stock")
    void confirm_shouldThrowInvalidState_whenReservationExpired() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 7);
        Reservation reservation = activeReservation("res-1", "SKU-1", 3, NOW.minusSeconds(1));
        when(reservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));
        when(productRepository.findById("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        assertThrows(InvalidReservationStateException.class, () -> service.confirm("res-1"));
        assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
        assertEquals(10, product.getAvailableQuantity());
    }

    @Test
    @DisplayName("Cancel releases held stock and marks reservation CANCELLED")
    void cancel_shouldReleaseStock_whenReservationIsActive() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 7);
        Reservation reservation = activeReservation("res-1", "SKU-1", 3, NOW.plusSeconds(600));
        when(reservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));
        when(productRepository.findById("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReservationResponse response = service.cancel("res-1");

        // Assert
        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
        assertEquals(10, product.getAvailableQuantity());
    }

    @Test
    @DisplayName("Cancel rejects a reservation that is not ACTIVE")
    void cancel_shouldThrowInvalidState_whenAlreadyCancelled() {
        // Arrange
        Reservation reservation = new Reservation(
                "res-1", "SKU-1", 3, ReservationStatus.CANCELLED, NOW, NOW.plusSeconds(600));
        when(reservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThrows(InvalidReservationStateException.class, () -> service.cancel("res-1"));
    }

    @Test
    @DisplayName("Expire releases stock for an ACTIVE reservation past TTL")
    void expireReservation_shouldReleaseStock_whenPastTtl() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 7);
        Reservation reservation = activeReservation("res-1", "SKU-1", 3, NOW.minusSeconds(1));
        when(reservationRepository.findById("res-1")).thenReturn(Optional.of(reservation));
        when(productRepository.findById("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReservationResponse response = service.expireReservation("res-1");

        // Assert
        assertEquals(ReservationStatus.EXPIRED, response.getStatus());
        assertEquals(10, product.getAvailableQuantity());
    }

    @Test
    @DisplayName("Expiry sweep processes all active expired reservations")
    void releaseExpiredReservations_shouldExpireAllActivePastTtl() {
        // Arrange
        Product product = new Product("SKU-1", "Widget", 5);
        Reservation expired = activeReservation("res-1", "SKU-1", 2, NOW.minusSeconds(1));
        when(reservationRepository.findActiveExpiredBefore(NOW)).thenReturn(List.of(expired));
        when(reservationRepository.findById("res-1")).thenReturn(Optional.of(expired));
        when(productRepository.findById("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.releaseExpiredReservations();

        // Assert
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertEquals(ReservationStatus.EXPIRED, captor.getValue().getStatus());
        assertEquals(7, product.getAvailableQuantity());
    }

    @Test
    @DisplayName("Confirm throws when reservation id is unknown")
    void confirm_shouldThrowNotFound_whenReservationMissing() {
        // Arrange
        when(reservationRepository.findById("missing")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ReservationNotFoundException.class, () -> service.confirm("missing"));
    }

    private static Reservation activeReservation(
            String id, String productId, int quantity, Instant expiresAt) {
        return new Reservation(id, productId, quantity, ReservationStatus.ACTIVE, NOW, expiresAt);
    }
}
