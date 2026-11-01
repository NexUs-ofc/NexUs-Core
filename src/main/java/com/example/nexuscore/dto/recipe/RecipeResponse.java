package com.example.nexuscore.dto.recipe;

import com.example.nexuscore.model.RecipeDifficulty;
import java.util.List;

public record RecipeResponse(
        String id,
        String title,
        Integer servingSize,
        String estimatedTime,
        RecipeDifficulty difficulty,
        List<RecipeIngredientResponse> ingredients,
        List<RecipeStepResponse> steps,
        String instructions,
        String createdBy
) {}
