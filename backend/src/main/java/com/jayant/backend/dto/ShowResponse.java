package com.jayant.backend.dto;

import java.time.LocalDateTime;

public record ShowResponse(Long id, String name, String venue, LocalDateTime showTime) {
}