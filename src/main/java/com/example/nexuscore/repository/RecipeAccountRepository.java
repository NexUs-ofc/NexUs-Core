package com.example.nexuscore.repository;

import com.example.nexuscore.model.RecipeAccount;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface RecipeAccountRepository extends MongoRepository<RecipeAccount, ObjectId> {

    List<RecipeAccount> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    Optional<RecipeAccount> findByRecipeIdAndAccountId(ObjectId recipeId, Integer accountId);
}
