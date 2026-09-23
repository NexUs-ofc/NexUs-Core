package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.notification.NotificationResponse;
import com.example.nexuscore.model.Notification;

public final class NotificationMapper {
    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId().toHexString(), notification.getTitle(), notification.getMessage(),
                notification.getType(), notification.getCreatedAt(), notification.getReadAt());
    }
}
