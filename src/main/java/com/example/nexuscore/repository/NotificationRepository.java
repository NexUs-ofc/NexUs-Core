package com.example.nexuscore.repository;

import com.example.nexuscore.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByProfileIdOrderByCreatedAtDesc(Integer profileId);

    Optional<Notification> findByIdAndProfileId(Integer id, Integer profileId);
}
