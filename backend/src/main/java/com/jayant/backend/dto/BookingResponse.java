package com.jayant.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        Long showId,
        String showName,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime holdExpiresAt,
        List<String> seats) {
}