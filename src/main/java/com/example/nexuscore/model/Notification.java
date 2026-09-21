package com.example.nexuscore.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "notifications")
public class Notification {

    @Id
    private ObjectId id;

    @Field("profile_id")
    private Integer profileId;

    private String title;

    private String message;

    private String type;

    @Field("created_at")
    private Instant createdAt;

    @Field("read_at")
    private Instant readAt;

    protected Notification() {
    }

    public ObjectId getId() {
        return id;
    }

    public Integer getProfileId() {
        return profileId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markAsRead(Instant instant) {
        if (readAt == null) {
            readAt = instant;
        }
    }
}
