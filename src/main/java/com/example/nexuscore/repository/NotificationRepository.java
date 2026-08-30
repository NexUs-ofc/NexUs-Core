package com.example.nexuscore.repository;

import com.example.nexuscore.model.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByProfileIdOrderByCreatedAtDesc(Integer profileId);
}
