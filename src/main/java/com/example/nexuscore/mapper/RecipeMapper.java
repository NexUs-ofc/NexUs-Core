package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.recipe.RecipeIngredientResponse;
import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.model.Recipe;

public final class RecipeMapper {
    private RecipeMapper() {
    }

    public static RecipeResponse toResponse(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId().toHexString(),
                recipe.getTitle(),
                recipe.getServingSize(),
                recipe.getIngredients().stream()
                        .map(ingredient -> new RecipeIngredientResponse(
                                ingredient.getFoodId(),
                                ingredient.getRequiredQuantity(),
                                ingredient.getMandatory(),
                                ingredient.getPossibleSubstitutes()))
                        .toList(),
                recipe.getInstructions(),
                recipe.getCreatedBy());
    }
}
