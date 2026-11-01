package com.example.nexuscore.repository;

import com.example.nexuscore.model.RecipeAccount;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RecipeAccountRepository extends MongoRepository<RecipeAccount, ObjectId> {

    List<RecipeAccount> findByAccountId(Integer accountId);

    boolean existsByRecipeIdAndAccountId(ObjectId recipeId, Integer accountId);

    void deleteByRecipeIdAndAccountId(ObjectId recipeId, Integer accountId);
}
