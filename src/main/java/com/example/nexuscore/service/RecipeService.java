package com.example.nexuscore.service;

import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.RecipeMapper;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.repository.RecipeRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final ActivityRecordService activityRecordService;

    public RecipeService(RecipeRepository recipeRepository, ActivityRecordService activityRecordService) {
        this.recipeRepository = recipeRepository;
        this.activityRecordService = activityRecordService;
    }

    public List<RecipeResponse> listSaved(Integer accountId) {
        return recipeRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(RecipeMapper::toResponse)
                .toList();
    }

    public void deleteSaved(Integer accountId, String recipeId) {
        ObjectId objectId = parseRecipeId(recipeId);
        Recipe recipe = recipeRepository.findByIdAndAccountId(objectId, accountId)
                .orElseThrow(() -> new NotFoundException("Receita nao encontrada: " + recipeId));
        recipeRepository.delete(recipe);
        activityRecordService.record(accountId, "saved_recipe_removed",
                "Removeu a receita salva " + recipe.getTitle());
    }

    private ObjectId parseRecipeId(String recipeId) {
        if (!ObjectId.isValid(recipeId)) {
            throw new NotFoundException("Receita nao encontrada: " + recipeId);
        }
        return new ObjectId(recipeId);
    }
}
