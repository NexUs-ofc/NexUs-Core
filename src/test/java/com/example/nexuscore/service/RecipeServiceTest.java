package com.example.nexuscore.service;

import com.example.nexuscore.dto.recipe.RecipeIngredientResponse;
import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.model.RecipeAccount;
import com.example.nexuscore.model.RecipeDifficulty;
import com.example.nexuscore.model.RecipeIngredient;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.RecipeAccountRepository;
import com.example.nexuscore.repository.RecipeRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecipeServiceTest {

    private static final ObjectId ID = new ObjectId("507f1f77bcf86cd799439011");
    private final RecipeRepository recipes = mock(RecipeRepository.class);
    private final RecipeAccountRepository recipeAccounts = mock(RecipeAccountRepository.class);
    private final ActivityRecordService activities = mock(ActivityRecordService.class);
    private final FoodRepository foods = mock(FoodRepository.class);
    private final RecipeService service = new RecipeService(recipes, activities, foods, recipeAccounts);

    @Test
    void listsOnlySavedRecipesForTheProfileAndResolvesFoodNamesInOneQuery() {
        Recipe first = recipe(ID, "Frango ao molho", List.of(ingredient(5, "500g")));
        ObjectId secondId = new ObjectId("507f1f77bcf86cd799439012");
        Recipe second = recipe(secondId, "Frango assado", List.of(ingredient(5, "1kg")));
        when(first.getCreatedAt()).thenReturn(Instant.parse("2026-10-24T12:00:00Z"));
        when(second.getCreatedAt()).thenReturn(Instant.parse("2026-10-23T12:00:00Z"));
        when(recipes.findByAccountIdOrIdIn(12, List.of())).thenReturn(List.of(second, first));
        Food chicken = food(5, "Peito de frango");
        when(foods.findAllById(List.of(5))).thenReturn(List.of(chicken));

        List<RecipeResponse> result = service.listSaved(12);

        assertEquals(List.of(ID.toHexString(), secondId.toHexString()),
                result.stream().map(RecipeResponse::id).toList());
        assertEquals("Peito de frango", result.get(0).ingredients().get(0).foodName());
        assertEquals("1kg", result.get(1).ingredients().get(0).requiredQuantity());
        verify(recipes).findByAccountIdOrIdIn(12, List.of());
        verify(foods).findAllById(List.of(5));
        verifyNoInteractions(activities);
    }

    @Test
    void returnsAnEmptyListWithoutLookingUpFoods() {
        when(recipes.findByAccountIdOrIdIn(12, List.of())).thenReturn(List.of());

        assertEquals(List.of(), service.listSaved(12));

        verifyNoInteractions(foods, activities);
    }

    @Test
    void loadsTheSavedRecipeWithItsIngredientsAndInstructions() {
        Recipe recipe = recipe(ID, "Frango ao molho", List.of(ingredient(5, "500g")));
        when(recipe.getEstimatedTime()).thenReturn("30 minutos");
        when(recipe.getDifficulty()).thenReturn(RecipeDifficulty.EASY);
        when(recipes.findByIdAndAccountId(ID, 12)).thenReturn(Optional.of(recipe));
        Food chicken = food(5, "Peito de frango");
        when(foods.findAllById(List.of(5))).thenReturn(List.of(chicken));

        RecipeResponse result = service.getSaved(12, ID.toHexString());

        assertEquals(ID.toHexString(), result.id());
        assertEquals("Frango ao molho", result.title());
        assertEquals(4, result.servingSize());
        assertEquals("30 minutos", result.estimatedTime());
        assertEquals(RecipeDifficulty.EASY, result.difficulty());
        assertEquals("Corte o frango.\nCozinhe por 15 minutos.", result.instructions());
        assertEquals("Cerita", result.createdBy());
        assertEquals(new RecipeIngredientResponse(5, "500g", true, List.of(7), "Peito de frango"),
                result.ingredients().get(0));
        verify(recipes).findByIdAndAccountId(ID, 12);
        verifyNoInteractions(activities);
    }

    @Test
    void preservesIngredientsWhenTheFoodIsUnavailable() {
        Recipe recipe = recipe(ID, "Frango ao molho", List.of(ingredient(5, "500g")));
        when(recipes.findByIdAndAccountId(ID, 12)).thenReturn(Optional.of(recipe));
        when(foods.findAllById(List.of(5))).thenReturn(List.of());

        RecipeIngredientResponse ingredient = service.getSaved(12, ID.toHexString()).ingredients().get(0);

        assertEquals(5, ingredient.foodId());
        assertEquals("500g", ingredient.requiredQuantity());
        assertNull(ingredient.foodName());
    }

    @Test
    void toleratesSavedRecipesWithoutIngredientsOrFoodIds() {
        Recipe first = recipe(ID, "Sem ingredientes", null);
        Recipe second = recipe(new ObjectId("507f1f77bcf86cd799439010"),
                "Sem alimento associado", List.of(ingredient(null, "A gosto")));
        when(recipes.findByAccountIdOrIdIn(12, List.of())).thenReturn(List.of(first, second));

        List<RecipeResponse> result = service.listSaved(12);

        assertEquals(List.of(), result.get(0).ingredients());
        assertNull(result.get(1).ingredients().get(0).foodName());
        verifyNoInteractions(foods);
    }

    @Test
    void rejectsInvalidIdsBeforeAccessingRepositories() {
        for (String id : new String[] {null, "", "not-an-object-id"}) {
            assertThrows(NotFoundException.class, () -> service.getSaved(12, id));
            assertThrows(NotFoundException.class, () -> service.deleteSaved(12, id));
        }

        verifyNoInteractions(recipes, recipeAccounts, foods, activities);
    }

    @Test
    void doesNotExposeOrDeleteAnotherProfilesRecipe() {
        when(recipes.findByIdAndAccountId(ID, 12)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getSaved(12, ID.toHexString()));
        assertThrows(NotFoundException.class, () -> service.deleteSaved(12, ID.toHexString()));

        verifyNoInteractions(foods, activities);
        verify(recipes, never()).delete(any());
        verify(recipes, never()).removeAccountOwnership(any(), any());
        verify(recipeAccounts, never()).deleteByRecipeIdAndAccountId(any(), any());
    }

    @Test
    void removesLegacyOwnershipWithoutDeletingTheRecipeDocument() {
        Recipe recipe = recipe(ID, "Frango ao molho", List.of());
        when(recipes.findByIdAndAccountId(ID, 12)).thenReturn(Optional.of(recipe));

        service.deleteSaved(12, ID.toHexString());

        verify(recipeAccounts).deleteByRecipeIdAndAccountId(ID, 12);
        verify(recipes).removeAccountOwnership(ID, 12);
        verify(recipes, never()).delete(any());
        verify(activities).record(12, "saved_recipe_removed", "Removeu a receita salva Frango ao molho");
        verifyNoInteractions(foods);
    }

    @Test
    void listsLinkedAndLegacyRecipesBySavedDateWithStableTies() {
        ObjectId secondId = new ObjectId("507f1f77bcf86cd799439012");
        ObjectId thirdId = new ObjectId("507f1f77bcf86cd799439013");
        Instant newest = Instant.parse("2026-10-24T12:00:00Z");
        Recipe legacy = recipe(ID, "Arroz", List.of());
        when(legacy.getCreatedAt()).thenReturn(newest.minusSeconds(60));
        Recipe linked = recipe(secondId, "Frango", List.of());
        when(linked.getCreatedAt()).thenReturn(newest.minusSeconds(120));
        when(linked.getEstimatedTime()).thenReturn("40 minutos");
        when(linked.getDifficulty()).thenReturn(RecipeDifficulty.MEDIUM);
        Recipe tied = recipe(thirdId, "Bolo", List.of());
        List<RecipeAccount> links = List.of(saved(secondId, newest.minusSeconds(180)),
                saved(thirdId, newest), saved(secondId, newest), saved(null, newest));
        when(recipeAccounts.findByAccountId(12)).thenReturn(links);
        when(recipes.findByAccountIdOrIdIn(12, List.of(secondId, thirdId)))
                .thenReturn(List.of(legacy, linked, tied));

        List<RecipeResponse> result = service.listSaved(12);

        assertEquals(List.of(thirdId.toHexString(), secondId.toHexString(), ID.toHexString()),
                result.stream().map(RecipeResponse::id).toList());
        assertEquals("40 minutos", result.get(1).estimatedTime());
        assertEquals(RecipeDifficulty.MEDIUM, result.get(1).difficulty());
        verify(recipeAccounts).findByAccountId(12);
        verifyNoInteractions(foods, activities);
    }

    @Test
    void usesRecipeDateWhenTheLinkHasNoSavedDateAndKeepsUndatedRecipesLast() {
        ObjectId secondId = new ObjectId("507f1f77bcf86cd799439012");
        Recipe dated = recipe(ID, "Arroz", List.of());
        when(dated.getCreatedAt()).thenReturn(Instant.parse("2026-10-24T12:00:00Z"));
        Recipe undated = recipe(secondId, "Bolo", List.of());
        List<RecipeAccount> links = List.of(saved(ID, null), saved(secondId, null));
        when(recipeAccounts.findByAccountId(12)).thenReturn(links);
        when(recipes.findByAccountIdOrIdIn(12, List.of(ID, secondId))).thenReturn(List.of(undated, dated));

        assertEquals(List.of(ID.toHexString(), secondId.toHexString()),
                service.listSaved(12).stream().map(RecipeResponse::id).toList());
    }

    @Test
    void loadsRecipesLinkedThroughRecipeAccountsWithOptionalMetadata() {
        Recipe recipe = recipe(ID, "Arroz", List.of());
        when(recipeAccounts.existsByRecipeIdAndAccountId(ID, 12)).thenReturn(true);
        when(recipes.findById(ID)).thenReturn(Optional.of(recipe));

        RecipeResponse result = service.getSaved(12, ID.toHexString());

        assertEquals(ID.toHexString(), result.id());
        assertEquals(4, result.servingSize());
        assertNull(result.estimatedTime());
        assertNull(result.difficulty());
        verify(recipes, never()).findByIdAndAccountId(any(), any());
        verifyNoInteractions(foods, activities);
    }

    @Test
    void removesOnlyTheAuthenticatedProfilesLink() {
        Recipe recipe = recipe(ID, "Arroz", List.of());
        when(recipeAccounts.existsByRecipeIdAndAccountId(ID, 12)).thenReturn(true);
        when(recipes.findById(ID)).thenReturn(Optional.of(recipe));

        service.deleteSaved(12, ID.toHexString());

        verify(recipeAccounts).deleteByRecipeIdAndAccountId(ID, 12);
        verify(recipes).removeAccountOwnership(ID, 12);
        verify(recipes, never()).delete(any());
        verify(activities).record(12, "saved_recipe_removed", "Removeu a receita salva Arroz");
        verifyNoInteractions(foods);
    }

    @Test
    void doesNotExposeOrRemoveDanglingLinks() {
        when(recipeAccounts.existsByRecipeIdAndAccountId(ID, 12)).thenReturn(true);
        when(recipes.findById(ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getSaved(12, ID.toHexString()));
        assertThrows(NotFoundException.class, () -> service.deleteSaved(12, ID.toHexString()));

        verify(recipeAccounts, never()).deleteByRecipeIdAndAccountId(any(), any());
        verify(recipes, never()).removeAccountOwnership(any(), any());
        verifyNoInteractions(foods, activities);
    }

    private RecipeAccount saved(ObjectId recipeId, Instant createdAt) {
        RecipeAccount saved = mock(RecipeAccount.class);
        when(saved.getRecipeId()).thenReturn(recipeId);
        when(saved.getCreatedAt()).thenReturn(createdAt);
        return saved;
    }

    private Recipe recipe(ObjectId id, String title, List<RecipeIngredient> ingredients) {
        Recipe recipe = mock(Recipe.class);
        when(recipe.getId()).thenReturn(id);
        when(recipe.getTitle()).thenReturn(title);
        when(recipe.getServingSize()).thenReturn(4);
        when(recipe.getIngredients()).thenReturn(ingredients);
        when(recipe.getInstructions()).thenReturn("Corte o frango.\nCozinhe por 15 minutos.");
        when(recipe.getCreatedBy()).thenReturn("Cerita");
        return recipe;
    }

    private RecipeIngredient ingredient(Integer foodId, String quantity) {
        RecipeIngredient ingredient = mock(RecipeIngredient.class);
        when(ingredient.getFoodId()).thenReturn(foodId);
        when(ingredient.getRequiredQuantity()).thenReturn(quantity);
        when(ingredient.getMandatory()).thenReturn(true);
        when(ingredient.getPossibleSubstitutes()).thenReturn(List.of(7));
        return ingredient;
    }

    private Food food(Integer id, String name) {
        Food food = mock(Food.class);
        when(food.getId()).thenReturn(id);
        when(food.getName()).thenReturn(name);
        return food;
    }
}
