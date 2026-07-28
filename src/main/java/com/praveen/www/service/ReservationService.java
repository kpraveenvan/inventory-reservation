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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe reservation lifecycle: reserve, confirm, cancel, and TTL expiry.
 * Uses per-product {@link ReentrantLock} to prevent overselling.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationProperties reservationProperties;
    private final Clock clock;
    private final ConcurrentHashMap<String, ReentrantLock> productLocks = new ConcurrentHashMap<>();

    public ReservationService(
            ProductRepository productRepository,
            ReservationRepository reservationRepository,
            ReservationProperties reservationProperties,
            Clock clock) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
        this.reservationProperties = reservationProperties;
        this.clock = clock;
    }

    public ReservationResponse reserve(ReserveRequest request) {
        String productId = request.getProductId();
        int quantity = request.getQuantity();
        ReentrantLock lock = lockFor(productId);
        lock.lock();
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

            if (product.getAvailableQuantity() < quantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for product " + productId
                                + ": requested=" + quantity
                                + ", available=" + product.getAvailableQuantity());
            }

            Instant now = Instant.now(clock);
            Instant expiresAt = now.plusSeconds(reservationProperties.getTtlSeconds());

            product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
            productRepository.save(product);

            Reservation reservation = new Reservation(
                    UUID.randomUUID().toString(),
                    productId,
                    quantity,
                    ReservationStatus.ACTIVE,
                    now,
                    expiresAt);
            reservationRepository.save(reservation);

            log.info("Reserved {} units of product {} as reservation {}",
                    quantity, productId, reservation.getId());
            return ReservationResponse.from(reservation);
        } finally {
            lock.unlock();
        }
    }

    public ReservationResponse confirm(String reservationId) {
        Reservation reservation = requireReservation(reservationId);
        ReentrantLock lock = lockFor(reservation.getProductId());
        lock.lock();
        try {
            reservation = requireReservation(reservationId);
            ensureActive(reservation);

            Instant now = Instant.now(clock);
            if (reservation.isExpiredAt(now)) {
                releaseStock(reservation);
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
                throw new InvalidReservationStateException(
                        "Reservation expired and cannot be confirmed: " + reservationId);
            }

            // Stock was already deducted at reserve time; confirm permanently consumes it.
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);

            log.info("Confirmed reservation {}", reservationId);
            return ReservationResponse.from(reservation);
        } finally {
            lock.unlock();
        }
    }

    public ReservationResponse cancel(String reservationId) {
        Reservation reservation = requireReservation(reservationId);
        ReentrantLock lock = lockFor(reservation.getProductId());
        lock.lock();
        try {
            reservation = requireReservation(reservationId);
            ensureActive(reservation);

            Instant now = Instant.now(clock);
            if (reservation.isExpiredAt(now)) {
                releaseStock(reservation);
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
                throw new InvalidReservationStateException(
                        "Reservation expired and cannot be cancelled: " + reservationId);
            }

            releaseStock(reservation);
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);

            log.info("Cancelled reservation {}", reservationId);
            return ReservationResponse.from(reservation);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases stock for active reservations whose TTL has elapsed.
     */
    @Scheduled(fixedDelayString = "${reservation.expiry-sweep-ms:5000}")
    public void releaseExpiredReservations() {
        Instant now = Instant.now(clock);
        List<Reservation> expired = reservationRepository.findActiveExpiredBefore(now);
        for (Reservation candidate : expired) {
            expireReservation(candidate.getId());
        }
    }

    /**
     * Expires a single reservation if it is still ACTIVE and past TTL.
     * Exposed for unit tests to exercise expiry without waiting on the scheduler.
     */
    public ReservationResponse expireReservation(String reservationId) {
        Reservation reservation = requireReservation(reservationId);
        ReentrantLock lock = lockFor(reservation.getProductId());
        lock.lock();
        try {
            reservation = requireReservation(reservationId);
            if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                return ReservationResponse.from(reservation);
            }

            Instant now = Instant.now(clock);
            if (!reservation.isExpiredAt(now)) {
                return ReservationResponse.from(reservation);
            }

            releaseStock(reservation);
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            log.info("Expired reservation {}", reservationId);
            return ReservationResponse.from(reservation);
        } finally {
            lock.unlock();
        }
    }

    private void releaseStock(Reservation reservation) {
        Product product = productRepository.findById(reservation.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found: " + reservation.getProductId()));
        product.setAvailableQuantity(product.getAvailableQuantity() + reservation.getQuantity());
        productRepository.save(product);
    }

    private void ensureActive(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(
                    "Reservation is not ACTIVE (status=" + reservation.getStatus()
                            + "): " + reservation.getId());
        }
    }

    private Reservation requireReservation(String reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found: " + reservationId));
    }

    private ReentrantLock lockFor(String productId) {
        return productLocks.computeIfAbsent(productId, id -> new ReentrantLock());
    }
}
