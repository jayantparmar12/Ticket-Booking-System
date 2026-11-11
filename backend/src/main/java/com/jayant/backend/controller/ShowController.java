package com.jayant.backend.controller;

import com.jayant.backend.dto.SeatResponse;
import com.jayant.backend.dto.ShowResponse;
import com.jayant.backend.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public List<ShowResponse> list() {
        return showService.upcoming();
    }

    @GetMapping("/{id}/seats")
    public List<SeatResponse> seats(@PathVariable Long id) {
        return showService.seats(id);
    }
}