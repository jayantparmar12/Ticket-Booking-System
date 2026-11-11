package com.jayant.backend.service;

import com.jayant.backend.dto.SeatResponse;
import com.jayant.backend.dto.ShowRequest;
import com.jayant.backend.dto.ShowResponse;
import com.jayant.backend.entity.Seat;
import com.jayant.backend.entity.SeatStatus;
import com.jayant.backend.entity.Show;
import com.jayant.backend.exception.ApiException;
import com.jayant.backend.repository.SeatRepository;
import com.jayant.backend.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public ShowResponse create(ShowRequest req) {
        Show show = showRepository.save(Show.builder()
                .name(req.name().trim())
                .venue(req.venue().trim())
                .showTime(req.showTime())
                .build());

        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < req.rows(); r++) {
            char row = (char) ('A' + r);
            for (int c = 1; c <= req.seatsPerRow(); c++) {
                seats.add(Seat.builder()
                        .show(show)
                        .label(row + String.valueOf(c))
                        .price(req.price())
                        .status(SeatStatus.AVAILABLE)
                        .build());
            }
        }
        seatRepository.saveAll(seats);
        return toResponse(show);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> upcoming() {
        return showRepository.findByShowTimeAfterOrderByShowTimeAsc(LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> seats(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Show not found");
        }
        LocalDateTime now = LocalDateTime.now();
        return seatRepository.findByShowIdOrderByIdAsc(showId).stream()
                .map(s -> new SeatResponse(s.getId(), s.getLabel(), s.getPrice(), effectiveStatus(s, now).name()))
                .toList();
    }

    private SeatStatus effectiveStatus(Seat seat, LocalDateTime now) {
        if (seat.getStatus() == SeatStatus.HELD && seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isBefore(now)) {
            return SeatStatus.AVAILABLE;
        }
        return seat.getStatus();
    }

    private ShowResponse toResponse(Show s) {
        return new ShowResponse(s.getId(), s.getName(), s.getVenue(), s.getShowTime());
    }
}