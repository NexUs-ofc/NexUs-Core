package com.example.nexuscore.dto.notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String title,
        String message,
        String type,
        Instant createdAt,
        Instant readAt
) {}
