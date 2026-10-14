package com.example.nexuscore.repository;

import com.example.nexuscore.model.ActivityRecord;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityRecordRepository extends MongoRepository<ActivityRecord, ObjectId> {
}
