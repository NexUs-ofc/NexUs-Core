package com.example.nexuscore.model;

public class EventRecipe {

    private String recipeId;

    protected EventRecipe() {}

    public EventRecipe(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeId() { return recipeId; }
}
