package com.example.nexuscore.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "recipes")
public class Recipe {

    @Id
    private ObjectId id;

    private String title;

    @Field("serving_size")
    private Integer servingSize;

    private List<RecipeIngredient> ingredients = new ArrayList<>();

    private String instructions;

    @Field("created_by")
    private String createdBy;

    @Field("account_id")
    private Integer accountId;

    @Field("created_at")
    private Instant createdAt;

    protected Recipe() {
    }

    public ObjectId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getServingSize() {
        return servingSize;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
