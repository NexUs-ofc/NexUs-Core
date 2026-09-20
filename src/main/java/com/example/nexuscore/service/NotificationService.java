package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.NotificationResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Notification;
import com.example.nexuscore.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public List<NotificationResponse> list(Integer profileId) {
        return repository.findByProfileIdOrderByCreatedAtDesc(profileId).stream().map(this::toResponse).toList();
    }

    public NotificationResponse get(Integer profileId, Integer id) {
        Notification notification = repository.findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new NotFoundException("Notificacao nao encontrada: " + id));
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getType(), notification.getCreatedAt(), notification.getReadAt());
    }
}
