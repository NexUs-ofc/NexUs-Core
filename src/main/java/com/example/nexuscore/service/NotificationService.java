package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.NotificationResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Notification;
import com.example.nexuscore.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notificacao nao encontrada: " + id));
        if (!notification.getProfile().getId().equals(profileId)) {
            throw new ForbiddenException("Notificacao nao pertence ao perfil autenticado");
        }
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getType(), notification.getCreatedAt(), notification.getReadAt());
    }
}
