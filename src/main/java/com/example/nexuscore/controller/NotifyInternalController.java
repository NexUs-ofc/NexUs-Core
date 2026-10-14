package com.example.nexuscore.controller;

import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreCandidate;
import com.example.nexuscore.dto.notification.NewStoreCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.service.NotificationCandidateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;

@RestController
@RequestMapping("/internal/notifications")
public class NotifyInternalController {

    private static final String TOKEN_HEADER = "X-Notify-Token";

    private final NotificationCandidateService candidateService;
    private final String sharedToken;

    public NotifyInternalController(NotificationCandidateService candidateService,
                                    @Value("${app.notify.shared-token:}") String sharedToken) {
        this.candidateService = candidateService;
        this.sharedToken = sharedToken;
    }

    @GetMapping("/products")
    public NotificationCandidatePage<ProductNotificationCandidate> products(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        authorize(token);
        return candidateService.products(from, to, page, size);
    }

    @GetMapping("/events")
    public NotificationCandidatePage<EventNotificationCandidate> events(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        authorize(token);
        return candidateService.events(from, to, page, size);
    }

    @GetMapping("/nearby-stores")
    public NotificationCandidatePage<NearbyStoreCandidate> nearbyStores(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        authorize(token);
        return candidateService.nearbyStores(radiusKm, page, size);
    }

    @GetMapping("/new-stores")
    public NotificationCandidatePage<NewStoreCandidate> newStores(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        authorize(token);
        return candidateService.newStores(from, to, radiusKm, page, size);
    }

    private void authorize(String token) {
        if (sharedToken.isBlank() || token == null
                || !MessageDigest.isEqual(sharedToken.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
