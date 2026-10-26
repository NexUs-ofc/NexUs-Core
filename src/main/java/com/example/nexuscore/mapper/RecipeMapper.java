package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.recipe.RecipeIngredientResponse;
import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.dto.recipe.RecipeStepResponse;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.model.RecipeIngredient;
import com.example.nexuscore.model.RecipeStep;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RecipeMapper {
    private RecipeMapper() {
    }

    public static RecipeResponse toResponse(Recipe recipe, Map<Integer, String> foodNames) {
        return new RecipeResponse(
                recipe.getId().toHexString(),
                recipe.getTitle(),
                recipe.getServingSize(),
                recipe.getEstimatedTime(),
                recipe.getDifficulty(),
                (recipe.getIngredients() == null ? List.<RecipeIngredient>of()
                        : recipe.getIngredients()).stream()
                        .map(ingredient -> new RecipeIngredientResponse(
                                ingredient.getFoodId(),
                                ingredient.getRequiredQuantity(),
                                ingredient.getMandatory(),
                                ingredient.getPossibleSubstitutes(),
                                ingredient.getFoodId() == null ? null : foodNames.get(ingredient.getFoodId())))
                        .toList(),
                (recipe.getSteps() == null ? List.<RecipeStep>of() : recipe.getSteps()).stream()
                        .filter(Objects::nonNull)
                        .map(step -> new RecipeStepResponse(step.getTitle(), step.getDescription()))
                        .toList(),
                recipe.getInstructions(),
                recipe.getCreatedBy());
    }
}
