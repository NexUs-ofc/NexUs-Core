package com.example.nexuscore.controller;

import com.example.nexuscore.dto.notification.NotificationResponse;
import com.example.nexuscore.service.NotificationService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;
    private final CurrentProfileResolver currentProfile;

    public NotificationController(NotificationService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return service.list(currentProfile.profileId().intValue());
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable String id) {
        return service.get(currentProfile.profileId().intValue(), id);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable String id) {
        return service.markAsRead(currentProfile.profileId().intValue(), id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        service.markAllAsRead(currentProfile.profileId().intValue());
    }
}
