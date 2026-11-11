package com.jayant.backend.controller;

import com.jayant.backend.dto.ShowRequest;
import com.jayant.backend.dto.ShowResponse;
import com.jayant.backend.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShowService showService;

    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@Valid @RequestBody ShowRequest req) {
        return showService.create(req);
    }
}