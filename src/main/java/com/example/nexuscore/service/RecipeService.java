package com.example.nexuscore.service;

import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.RecipeMapper;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.model.RecipeIngredient;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.RecipeRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ActivityRecordService activityRecordService;
    private final FoodRepository foodRepository;

    public RecipeService(RecipeRepository recipeRepository, ActivityRecordService activityRecordService,
                         FoodRepository foodRepository) {
        this.recipeRepository = recipeRepository;
        this.activityRecordService = activityRecordService;
        this.foodRepository = foodRepository;
    }

    public List<RecipeResponse> listSaved(Integer accountId) {
        List<Recipe> recipes = recipeRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        Map<Integer, String> foodNames = foodNames(recipes);
        return recipes.stream()
                .map(recipe -> RecipeMapper.toResponse(recipe, foodNames))
                .toList();
    }

    public RecipeResponse getSaved(Integer accountId, String recipeId) {
        Recipe recipe = findSaved(accountId, recipeId);
        return RecipeMapper.toResponse(recipe, foodNames(List.of(recipe)));
    }

    public void deleteSaved(Integer accountId, String recipeId) {
        Recipe recipe = findSaved(accountId, recipeId);
        recipeRepository.delete(recipe);
        activityRecordService.record(accountId, "saved_recipe_removed",
                "Removeu a receita salva " + recipe.getTitle());
    }

    private Recipe findSaved(Integer accountId, String recipeId) {
        ObjectId objectId = parseRecipeId(recipeId);
        return recipeRepository.findByIdAndAccountId(objectId, accountId)
                .orElseThrow(() -> new NotFoundException("Receita nao encontrada: " + recipeId));
    }

    private Map<Integer, String> foodNames(List<Recipe> recipes) {
        List<Integer> foodIds = recipes.stream()
                .filter(recipe -> recipe.getIngredients() != null)
                .flatMap(recipe -> recipe.getIngredients().stream())
                .map(RecipeIngredient::getFoodId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (foodIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> foodNames = new HashMap<>();
        for (Food food : foodRepository.findAllById(foodIds)) {
            foodNames.put(food.getId(), food.getName());
        }
        return foodNames;
    }

    private ObjectId parseRecipeId(String recipeId) {
        if (recipeId == null || !ObjectId.isValid(recipeId)) {
            throw new NotFoundException("Receita nao encontrada: " + recipeId);
        }
        return new ObjectId(recipeId);
    }
}
