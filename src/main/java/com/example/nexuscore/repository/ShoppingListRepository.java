package com.example.nexuscore.repository;

import com.example.nexuscore.model.ShoppingList;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface ShoppingListRepository extends MongoRepository<ShoppingList, String> {

    List<ShoppingList> findByHouseholdId(Integer householdId);

    @Query(value = "{'date': {'$gte': ?0, '$lte': ?1}}",
            fields = "{'household_id': 1, 'title': 1, 'date': 1, 'array_list.checked': 1}")
    Slice<ShoppingList> findNotificationCandidates(LocalDate from, LocalDate to, Pageable pageable);
}
