package com.example.nexuscore.service;

import com.example.nexuscore.dto.recipe.RecipeIngredientResponse;
import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.model.RecipeIngredient;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.RecipeRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
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
    private final ActivityRecordService activities = mock(ActivityRecordService.class);
    private final FoodRepository foods = mock(FoodRepository.class);
    private final RecipeService service = new RecipeService(recipes, activities, foods);

    @Test
    void listsOnlySavedRecipesForTheProfileAndResolvesFoodNamesInOneQuery() {
        Recipe first = recipe(ID, "Frango ao molho", List.of(ingredient(5, "500g")));
        ObjectId secondId = new ObjectId("507f1f77bcf86cd799439012");
        Recipe second = recipe(secondId, "Frango assado", List.of(ingredient(5, "1kg")));
        when(recipes.findByAccountIdOrderByCreatedAtDesc(12)).thenReturn(List.of(first, second));
        Food chicken = food(5, "Peito de frango");
        when(foods.findAllById(List.of(5))).thenReturn(List.of(chicken));

        List<RecipeResponse> result = service.listSaved(12);

        assertEquals(List.of(ID.toHexString(), secondId.toHexString()),
                result.stream().map(RecipeResponse::id).toList());
        assertEquals("Peito de frango", result.get(0).ingredients().get(0).foodName());
        assertEquals("1kg", result.get(1).ingredients().get(0).requiredQuantity());
        verify(recipes).findByAccountIdOrderByCreatedAtDesc(12);
        verify(foods).findAllById(List.of(5));
        verifyNoInteractions(activities);
    }

    @Test
    void returnsAnEmptyListWithoutLookingUpFoods() {
        when(recipes.findByAccountIdOrderByCreatedAtDesc(12)).thenReturn(List.of());

        assertEquals(List.of(), service.listSaved(12));

        verifyNoInteractions(foods, activities);
    }

    @Test
    void loadsTheSavedRecipeWithItsIngredientsAndInstructions() {
        Recipe recipe = recipe(ID, "Frango ao molho", List.of(ingredient(5, "500g")));
        when(recipes.findByIdAndAccountId(ID, 12)).thenReturn(Optional.of(recipe));
        Food chicken = food(5, "Peito de frango");
        when(foods.findAllById(List.of(5))).thenReturn(List.of(chicken));

        RecipeResponse result = service.getSaved(12, ID.toHexString());

        assertEquals(ID.toHexString(), result.id());
        assertEquals("Frango ao molho", result.title());
        assertEquals(4, result.servingSize());
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
        Recipe second = recipe(new ObjectId(), "Sem alimento associado", List.of(ingredient(null, "A gosto")));
        when(recipes.findByAccountIdOrderByCreatedAtDesc(12)).thenReturn(List.of(first, second));

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

        verifyNoInteractions(recipes, foods, activities);
    }

    @Test
    void doesNotExposeOrDeleteAnotherProfilesRecipe() {
        when(recipes.findByIdAndAccountId(ID, 12)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getSaved(12, ID.toHexString()));
        assertThrows(NotFoundException.class, () -> service.deleteSaved(12, ID.toHexString()));

        verifyNoInteractions(foods, activities);
        verify(recipes, never()).delete(any());
    }

    @Test
    void deletesOnlyTheSavedRecipeOwnedByTheProfile() {
        Recipe recipe = recipe(ID, "Frango ao molho", List.of());
        when(recipes.findByIdAndAccountId(ID, 12)).thenReturn(Optional.of(recipe));

        service.deleteSaved(12, ID.toHexString());

        verify(recipes).delete(recipe);
        verify(activities).record(12, "saved_recipe_removed", "Removeu a receita salva Frango ao molho");
        verifyNoInteractions(foods);
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
