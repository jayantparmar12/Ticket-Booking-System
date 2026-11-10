package com.jayant.backend.dto;

import java.math.BigDecimal;

public record SeatResponse(Long id, String label, BigDecimal price, String status) {
}