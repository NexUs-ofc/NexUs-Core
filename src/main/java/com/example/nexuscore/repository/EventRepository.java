package com.example.nexuscore.repository;

import com.example.nexuscore.model.Event;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findByHouseholdId(Integer householdId);
}
