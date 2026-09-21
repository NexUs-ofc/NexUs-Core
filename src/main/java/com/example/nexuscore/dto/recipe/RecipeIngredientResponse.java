package com.example.nexuscore.dto.recipe;

import java.util.List;

public record RecipeIngredientResponse(
        Integer foodId,
        String requiredQuantity,
        Boolean mandatory,
        List<Integer> possibleSubstitutes
) {}
