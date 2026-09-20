package com.example.nexuscore.model;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.ArrayList;
import java.util.List;

public class EventRecipe {

    @Field("recipe_id")
    private ObjectId recipeId;

    private String title;

    private List<EventRecipeIngredient> ingredients = new ArrayList<>();

    protected EventRecipe() {
    }

    public EventRecipe(ObjectId recipeId, String title) {
        this.recipeId = recipeId;
        this.title = title;
    }

    public ObjectId getRecipeId() {
        return recipeId;
    }

    public String getTitle() {
        return title;
    }

    public List<EventRecipeIngredient> getIngredients() {
        return ingredients;
    }
}
