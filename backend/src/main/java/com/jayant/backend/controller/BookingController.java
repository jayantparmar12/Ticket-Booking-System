package com.jayant.backend.controller;

import com.jayant.backend.dto.BookingResponse;
import com.jayant.backend.dto.HoldRequest;
import com.jayant.backend.dto.PayRequest;
import com.jayant.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse hold(Authentication auth, @Valid @RequestBody HoldRequest req) {
        return bookingService.hold(auth.getName(), req);
    }

    @PostMapping("/{id}/pay")
    public BookingResponse pay(Authentication auth, @PathVariable Long id, @RequestBody PayRequest req) {
        return bookingService.pay(auth.getName(), id, req.success());
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(Authentication auth, @PathVariable Long id) {
        return bookingService.cancel(auth.getName(), id);
    }

    @GetMapping("/my")
    public List<BookingResponse> mine(Authentication auth) {
        return bookingService.myBookings(auth.getName());
    }
}