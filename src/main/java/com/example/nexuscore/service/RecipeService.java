package com.example.nexuscore.service;

import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.RecipeMapper;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.model.RecipeAccount;
import com.example.nexuscore.repository.RecipeAccountRepository;
import com.example.nexuscore.repository.RecipeRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeAccountRepository recipeAccountRepository;

    public RecipeService(RecipeRepository recipeRepository, RecipeAccountRepository recipeAccountRepository) {
        this.recipeRepository = recipeRepository;
        this.recipeAccountRepository = recipeAccountRepository;
    }

    public List<RecipeResponse> listSaved(Integer accountId) {
        List<RecipeAccount> links = recipeAccountRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        List<ObjectId> recipeIds = links.stream().map(RecipeAccount::getRecipeId).toList();
        Map<ObjectId, Recipe> recipesById = recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        return links.stream()
                .map(link -> recipesById.get(link.getRecipeId()))
                .filter(recipe -> recipe != null)
                .map(RecipeMapper::toResponse)
                .toList();
    }

    public RecipeResponse favorite(Integer accountId, String recipeId) {
        ObjectId objectId = parseRecipeId(recipeId);
        Recipe recipe = recipeRepository.findById(objectId)
                .orElseThrow(() -> new NotFoundException("Receita nao encontrada: " + recipeId));

        recipeAccountRepository.findByRecipeIdAndAccountId(objectId, accountId)
                .orElseGet(() -> recipeAccountRepository.save(new RecipeAccount(objectId, accountId)));
        return RecipeMapper.toResponse(recipe);
    }

    public void unfavorite(Integer accountId, String recipeId) {
        ObjectId objectId = parseRecipeId(recipeId);
        recipeAccountRepository.findByRecipeIdAndAccountId(objectId, accountId)
                .ifPresent(recipeAccountRepository::delete);
    }

    private ObjectId parseRecipeId(String recipeId) {
        if (!ObjectId.isValid(recipeId)) {
            throw new NotFoundException("Receita nao encontrada: " + recipeId);
        }
        return new ObjectId(recipeId);
    }
}
