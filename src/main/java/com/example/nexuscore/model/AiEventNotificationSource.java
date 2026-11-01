package com.example.nexuscore.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "events")
@CompoundIndex(name = "idx_events_notification_date", def = "{'date': 1, '_id': 1}")
public class AiEventNotificationSource {

    @Id
    private String id;

    @Field("household_id")
    private Integer householdId;

    private String title;
    private Instant date;

    protected AiEventNotificationSource() {
    }

    public String getId() {
        return id;
    }

    public Integer getHouseholdId() {
        return householdId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getDate() {
        return date;
    }
}
