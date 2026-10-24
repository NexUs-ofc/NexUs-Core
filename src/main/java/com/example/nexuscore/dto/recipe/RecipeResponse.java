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
        String instructions,
        String createdBy
) {}
