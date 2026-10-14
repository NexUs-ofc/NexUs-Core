package com.example.nexuscore.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "records")
@CompoundIndex(name = "idx_records_account_date", def = "{'account_id': 1, 'date': -1}")
public class ActivityRecord {

    @Id
    private ObjectId id;

    @Field("account_id")
    private Integer accountId;

    @Field("event_type")
    private String eventType;

    private String details;

    private Instant date;

    protected ActivityRecord() {
    }

    public ActivityRecord(Integer accountId, String eventType, String details) {
        this.accountId = accountId;
        this.eventType = eventType;
        this.details = details;
        this.date = Instant.now();
    }

    public Integer getAccountId() {
        return accountId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDetails() {
        return details;
    }

    public Instant getDate() {
        return date;
    }
}
