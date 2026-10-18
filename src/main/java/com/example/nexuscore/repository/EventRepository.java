package com.example.nexuscore.repository;

import com.example.nexuscore.model.Event;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, ObjectId> {

    List<Event> findByHouseholdId(Integer householdId);

    @Query(value = "{ 'date': { '$gte': ?0, '$lt': ?1 } }", sort = "{ 'date': 1, '_id': 1 }")
    Slice<Event> findByDateGreaterThanEqualAndDateLessThanOrderByDateAscIdAsc(
            LocalDateTime from, LocalDateTime until, Pageable pageable);
}
