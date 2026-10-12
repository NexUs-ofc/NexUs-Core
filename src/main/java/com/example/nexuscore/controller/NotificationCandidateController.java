package com.example.nexuscore.controller;

import com.example.nexuscore.dto.notification.CandidatePageResponse;
import com.example.nexuscore.dto.notification.EventCandidateResponse;
import com.example.nexuscore.dto.notification.ProductCandidateResponse;
import com.example.nexuscore.service.NotificationCandidateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/internal/notifications")
public class NotificationCandidateController {

    private final NotificationCandidateService service;

    public NotificationCandidateController(NotificationCandidateService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public CandidatePageResponse<ProductCandidateResponse> products(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.products(from, to, page, size);
    }

    @GetMapping("/events")
    public CandidatePageResponse<EventCandidateResponse> events(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.events(from, to, page, size);
    }
}
