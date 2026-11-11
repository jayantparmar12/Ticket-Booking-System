package com.jayant.backend.service;

import com.jayant.backend.dto.BookingResponse;
import com.jayant.backend.dto.HoldRequest;
import com.jayant.backend.entity.*;
import com.jayant.backend.exception.ApiException;
import com.jayant.backend.repository.BookingRepository;
import com.jayant.backend.repository.SeatRepository;
import com.jayant.backend.repository.ShowRepository;
import com.jayant.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;

    @Value("${booking.hold-minutes:5}")
    private long holdMinutes;

    @Transactional
    public BookingResponse hold(String email, HoldRequest req) {
        User user = findUser(email);
        Show show = showRepository.findById(req.showId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Show not found"));
        LocalDateTime now = LocalDateTime.now();
        if (show.getShowTime().isBefore(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Show already started");
        }

        Set<Long> ids = new HashSet<>(req.seatIds());
        List<Seat> seats = seatRepository.lockSeats(show.getId(), ids);
        if (seats.size() != ids.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid seat selection");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Seat seat : seats) {
            if (!isFree(seat, now)) {
                throw new ApiException(HttpStatus.CONFLICT, "Seat " + seat.getLabel() + " is not available");
            }
            total = total.add(seat.getPrice());
        }

        LocalDateTime expiry = now.plusMinutes(holdMinutes);
        Booking booking = bookingRepository.save(Booking.builder()
                .user(user)
                .show(show)
                .status(BookingStatus.PENDING)
                .totalAmount(total)
                .createdAt(now)
                .holdExpiresAt(expiry)
                .seats(new ArrayList<>())
                .build());

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.HELD);
            seat.setBooking(booking);
            seat.setHoldExpiresAt(expiry);
        }
        seatRepository.saveAll(seats);
        booking.setSeats(seats);
        return toResponse(booking);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public BookingResponse pay(String email, Long bookingId, boolean success) {
        Booking booking = findOwned(email, bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking is " + booking.getStatus());
        }

        List<Seat> seats = seatRepository.lockByBooking(bookingId);

        if (booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            seats.forEach(this::release);
            booking.setStatus(BookingStatus.EXPIRED);
            throw new ApiException(HttpStatus.GONE, "Hold expired, please select seats again");
        }

        if (success) {
            for (Seat seat : seats) {
                seat.setStatus(SeatStatus.BOOKED);
                seat.setHoldExpiresAt(null);
            }
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setHoldExpiresAt(null);
        } else {
            seats.forEach(this::release);
            booking.setStatus(BookingStatus.CANCELLED);
        }
        booking.setSeats(seats);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(String email, Long bookingId) {
        Booking booking = findOwned(email, bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking is already " + booking.getStatus());
        }
        List<Seat> seats = seatRepository.lockByBooking(bookingId);
        List<String> labels = seats.stream().map(Seat::getLabel).sorted().toList();
        seats.forEach(this::release);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setHoldExpiresAt(null);
        return new BookingResponse(
                booking.getId(),
                booking.getShow().getId(),
                booking.getShow().getName(),
                booking.getStatus().name(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                booking.getHoldExpiresAt(),
                labels);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String email) {
        User user = findUser(email);
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public int releaseExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expired = bookingRepository.findByStatusAndHoldExpiresAtBefore(BookingStatus.PENDING, now);
        for (Booking booking : expired) {
            for (Seat seat : booking.getSeats()) {
                if (seat.getStatus() == SeatStatus.HELD) {
                    release(seat);
                }
            }
            booking.setStatus(BookingStatus.EXPIRED);
        }
        return expired.size();
    }

    private boolean isFree(Seat seat, LocalDateTime now) {
        if (seat.getStatus() == SeatStatus.AVAILABLE) {
            return true;
        }
        return seat.getStatus() == SeatStatus.HELD
                && seat.getHoldExpiresAt() != null
                && seat.getHoldExpiresAt().isBefore(now);
    }

    private void release(Seat seat) {
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setBooking(null);
        seat.setHoldExpiresAt(null);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Booking findOwned(String email, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getUser().getEmail().equals(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your booking");
        }
        return booking;
    }

    private BookingResponse toResponse(Booking b) {
        List<String> labels = b.getSeats().stream().map(Seat::getLabel).sorted().toList();
        return new BookingResponse(
                b.getId(),
                b.getShow().getId(),
                b.getShow().getName(),
                b.getStatus().name(),
                b.getTotalAmount(),
                b.getCreatedAt(),
                b.getHoldExpiresAt(),
                labels);
    }
}