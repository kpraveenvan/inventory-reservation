package com.praveen.www.web;

import com.praveen.www.dto.ReservationResponse;
import com.praveen.www.dto.ReserveRequest;
import com.praveen.www.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for creating, confirming, and cancelling reservations.
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        return reservationService.reserve(request);
    }

    @PostMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable String id) {
        return reservationService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable String id) {
        return reservationService.cancel(id);
    }
}
