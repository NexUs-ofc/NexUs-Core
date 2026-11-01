package com.example.nexuscore.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

@Document(collection = "recipe_accounts")
@CompoundIndex(name = "idx_recipe_accounts_account_recipe", def = "{'id_account': 1, 'id_recipe': 1}")
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

    public ObjectId getId() {
        return id;
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
