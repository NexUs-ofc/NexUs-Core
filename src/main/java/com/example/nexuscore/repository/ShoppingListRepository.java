package com.example.nexuscore.repository;

import com.example.nexuscore.model.ShoppingList;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ShoppingListRepository extends MongoRepository<ShoppingList, String> {

    List<ShoppingList> findByHouseholdId(Integer householdId);
}
