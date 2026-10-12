package com.example.nexuscore.repository;

import com.example.nexuscore.model.Event;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, ObjectId> {

    List<Event> findByHouseholdId(Integer householdId);

    Slice<Event> findByDateGreaterThanEqualAndDateLessThan(LocalDateTime from, LocalDateTime until, Pageable pageable);
}
