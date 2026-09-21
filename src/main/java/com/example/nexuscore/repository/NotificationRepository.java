package com.example.nexuscore.repository;

import com.example.nexuscore.model.Notification;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, ObjectId> {

    List<Notification> findByProfileIdOrderByCreatedAtDesc(Integer profileId);

    Optional<Notification> findByIdAndProfileId(ObjectId id, Integer profileId);

    @Query("{ 'profile_id': ?0, 'read_at': null }")
    @Update("{ '$set': { 'read_at': ?1 } }")
    long markAllAsRead(Integer profileId, Instant readAt);
}
