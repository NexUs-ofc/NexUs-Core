package com.example.nexuscore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nexuscore.dto.notification.NotificationResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Notification;
import com.example.nexuscore.repository.NotificationRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;

class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(repository);

    @Test
    void rejectsInvalidObjectId() {
        assertThrows(NotFoundException.class, () -> service.get(12, "invalid"));

        verify(repository, never()).findByIdAndProfileId(any(), any());
    }

    @Test
    void scopesNotificationByProfile() {
        ObjectId id = new ObjectId();
        when(repository.findByIdAndProfileId(id, 12)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(12, id.toHexString()));
        verify(repository).findByIdAndProfileId(id, 12);
    }

    @Test
    void marksOwnedNotificationAsRead() {
        ObjectId id = new ObjectId();
        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(id);
        when(repository.findByIdAndProfileId(id, 12)).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        NotificationResponse response = service.markAsRead(12, id.toHexString());

        assertEquals(id.toHexString(), response.id());
        verify(notification).markAsRead(any(Instant.class));
        verify(repository).save(notification);
    }

    @Test
    void marksAllUnreadNotificationsForProfile() {
        service.markAllAsRead(12);

        verify(repository).markAllAsRead(eq(12), any(Instant.class));
    }
}
