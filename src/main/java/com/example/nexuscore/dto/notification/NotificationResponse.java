package com.example.nexuscore.dto.notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Integer id,
        String title,
        String message,
        String type,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}
