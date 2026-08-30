package com.example.nexuscore.repository;

import com.example.nexuscore.model.ShoppingList;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShoppingListRepository extends MongoRepository<ShoppingList, String> {

    List<ShoppingList> findByHouseholdId(Integer householdId);
}
