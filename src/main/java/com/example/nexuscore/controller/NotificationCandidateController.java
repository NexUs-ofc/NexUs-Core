package com.example.nexuscore.controller;

import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreCandidate;
import com.example.nexuscore.dto.notification.NewStoreCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.service.NotificationCandidateService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/internal/notifications")
@SecurityRequirement(name = "NotifyTokenAuth")
public class NotificationCandidateController {

    private final NotificationCandidateService service;

    public NotificationCandidateController(NotificationCandidateService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public NotificationCandidatePage<ProductNotificationCandidate> products(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.products(from, to, page, size);
    }

    @GetMapping("/events")
    public NotificationCandidatePage<EventNotificationCandidate> events(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.events(from, to, page, size);
    }

    @GetMapping("/nearby-stores")
    public NotificationCandidatePage<NearbyStoreCandidate> nearbyStores(
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.nearbyStores(radiusKm, page, size);
    }

    @GetMapping("/new-stores")
    public NotificationCandidatePage<NewStoreCandidate> newStores(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.newStores(from, to, radiusKm, page, size);
    }
}
