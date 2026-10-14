package com.example.nexuscore.repository;

import com.example.nexuscore.model.Recipe;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends MongoRepository<Recipe, ObjectId> {

    List<Recipe> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    Optional<Recipe> findByIdAndAccountId(ObjectId id, Integer accountId);
}
