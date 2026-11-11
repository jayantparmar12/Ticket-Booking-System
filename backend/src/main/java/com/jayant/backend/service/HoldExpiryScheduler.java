package com.jayant.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HoldExpiryScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedRate = 60000)
    public void releaseExpiredHolds() {
        try {
            int count = bookingService.releaseExpired();
            if (count > 0) {
                log.info("Released {} expired holds", count);
            }
        } catch (Exception e) {
            log.warn("Hold expiry run failed: {}", e.getMessage());
        }
    }
}