package com.example.nexuscore.dto.recipe;

import java.util.List;

public record RecipeResponse(
        String id,
        String title,
        Integer servingSize,
        List<RecipeIngredientResponse> ingredients,
        String instructions,
        String createdBy
) {}
