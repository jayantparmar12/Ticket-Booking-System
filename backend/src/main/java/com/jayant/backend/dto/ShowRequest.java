package com.jayant.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowRequest(
        @NotBlank String name,
        @NotBlank String venue,
        @NotNull @Future LocalDateTime showTime,
        @Min(1) @Max(26) int rows,
        @Min(1) @Max(50) int seatsPerRow,
        @NotNull @DecimalMin("0") BigDecimal price) {
}