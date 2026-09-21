package com.example.nexuscore.repository;

import com.example.nexuscore.model.Event;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, ObjectId> {

    List<Event> findByHouseholdId(Integer householdId);
}
