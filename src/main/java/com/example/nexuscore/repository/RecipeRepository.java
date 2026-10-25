package com.example.nexuscore.repository;

import com.example.nexuscore.model.Recipe;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends MongoRepository<Recipe, ObjectId> {

    List<Recipe> findByAccountIdOrIdIn(Integer accountId, List<ObjectId> recipeIds);

    Optional<Recipe> findByIdAndAccountId(ObjectId id, Integer accountId);

    @Query("{'_id': ?0, 'account_id': ?1}")
    @Update("{'$unset': {'account_id': ''}}")
    void removeAccountOwnership(ObjectId id, Integer accountId);
}
