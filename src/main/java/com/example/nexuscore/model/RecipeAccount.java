package com.example.nexuscore.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "recipe_accounts")
public class RecipeAccount {

    @Id
    private ObjectId id;

    @Field("id_recipe")
    private ObjectId recipeId;

    @Field("id_account")
    private Integer accountId;

    @Field("created_at")
    private Instant createdAt;

    protected RecipeAccount() {
    }

    public RecipeAccount(ObjectId recipeId, Integer accountId) {
        this.recipeId = recipeId;
        this.accountId = accountId;
        this.createdAt = Instant.now();
    }

    public ObjectId getRecipeId() {
        return recipeId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
