package com.example.nexuscore.repository;

import com.example.nexuscore.model.Recipe;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecipeRepository extends MongoRepository<Recipe, ObjectId> {
}
