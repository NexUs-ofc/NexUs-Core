package com.example.nexuscore.repository;

import com.example.nexuscore.model.AiEventNotificationSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.Repository;
import java.time.Instant;

public interface AiEventNotificationRepository extends Repository<AiEventNotificationSource, String> {

    @Query(value = "{'date': {'$gte': ?0, '$lt': ?1}}", fields = "{'household_id': 1, 'title': 1, 'date': 1}")
    Slice<AiEventNotificationSource> findNotificationCandidates(Instant from, Instant until, Pageable pageable);
}
