package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.NotificationResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.NotificationMapper;
import com.example.nexuscore.model.Notification;
import com.example.nexuscore.repository.NotificationRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public List<NotificationResponse> list(Integer profileId) {
        return repository.findByProfileIdOrderByCreatedAtDesc(profileId).stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }

    public NotificationResponse get(Integer profileId, String id) {
        Notification notification = findOwnedNotification(profileId, id);
        return NotificationMapper.toResponse(notification);
    }

    public NotificationResponse markAsRead(Integer profileId, String id) {
        Notification notification = findOwnedNotification(profileId, id);
        notification.markAsRead(Instant.now());
        return NotificationMapper.toResponse(repository.save(notification));
    }

    public void markAllAsRead(Integer profileId) {
        repository.markAllAsRead(profileId, Instant.now());
    }

    private Notification findOwnedNotification(Integer profileId, String id) {
        if (!ObjectId.isValid(id)) {
            throw new NotFoundException("Notificacao nao encontrada: " + id);
        }
        ObjectId objectId = new ObjectId(id);
        return repository.findByIdAndProfileId(objectId, profileId)
                .orElseThrow(() -> new NotFoundException("Notificacao nao encontrada: " + id));
    }

}
