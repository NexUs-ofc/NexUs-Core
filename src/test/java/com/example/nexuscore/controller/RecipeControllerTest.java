package com.example.nexuscore.controller;

import com.example.nexuscore.config.CoreConfiguration;
import com.example.nexuscore.config.SecurityConfig;
import com.example.nexuscore.dto.recipe.RecipeIngredientResponse;
import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.RecipeDifficulty;
import com.example.nexuscore.service.RecipeService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecipeController.class, properties = {
    "app.auth.issuer=nexus-core-test",
    "app.auth.jwt-secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import({SecurityConfig.class, CoreConfiguration.class, CurrentProfileResolver.class,
        RecipeControllerTest.TestSecurityConfiguration.class})
class RecipeControllerTest {

    private static final String ID = "507f1f77bcf86cd799439011";
    private static final String RECIPES = "/api/recipes";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private RecipeService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class))
                .build();
        when(jwtDecoder.decode("profile-token")).thenReturn(Jwt.withTokenValue("profile-token")
                .header("alg", "HS256").subject("12").claim("profile_type", "HOUSEHOLD").build());
    }

    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Test
    void listsRecipesForTheAuthenticatedProfile() throws Exception {
        when(service.listSaved(12)).thenReturn(List.of(recipe()));

        mockMvc.perform(get(RECIPES).header("Authorization", "Bearer profile-token")
                        .param("accountId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID))
                .andExpect(jsonPath("$[0].title").value("Frango ao molho"))
                .andExpect(jsonPath("$[0].servingSize").value(4))
                .andExpect(jsonPath("$[0].estimatedTime").value("30 minutos"))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$[0].ingredients[0].foodName").value("Peito de frango"));

        verify(service).listSaved(12);
    }

    @Test
    void returnsSavedRecipeDetailsForTheAuthenticatedProfile() throws Exception {
        when(service.getSaved(12, ID)).thenReturn(recipe());

        mockMvc.perform(get(RECIPES + "/" + ID).header("Authorization", "Bearer profile-token")
                        .param("accountId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.servingSize").value(4))
                .andExpect(jsonPath("$.estimatedTime").value("30 minutos"))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.ingredients[0].foodId").value(5))
                .andExpect(jsonPath("$.ingredients[0].foodName").value("Peito de frango"))
                .andExpect(jsonPath("$.ingredients[0].requiredQuantity").value("500g"))
                .andExpect(jsonPath("$.ingredients[0].mandatory").value(true))
                .andExpect(jsonPath("$.ingredients[0].possibleSubstitutes[0]").value(7))
                .andExpect(jsonPath("$.instructions").value("Corte o frango.\nCozinhe por 15 minutos."))
                .andExpect(jsonPath("$.createdBy").value("Cerita"));

        verify(service).getSaved(12, ID);
    }

    @Test
    void returnsRecipesWithoutOptionalMetadata() throws Exception {
        RecipeResponse legacy = new RecipeResponse(ID, "Arroz", 4, null, null, List.of(), "Cozinhe.", "ai");
        when(service.listSaved(12)).thenReturn(List.of(legacy));
        when(service.getSaved(12, ID)).thenReturn(legacy);

        mockMvc.perform(get(RECIPES).header("Authorization", "Bearer profile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].servingSize").value(4))
                .andExpect(jsonPath("$[0].estimatedTime").doesNotExist())
                .andExpect(jsonPath("$[0].difficulty").doesNotExist());
        mockMvc.perform(get(RECIPES + "/" + ID).header("Authorization", "Bearer profile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedTime").doesNotExist())
                .andExpect(jsonPath("$.difficulty").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(RecipeDifficulty.class)
    void exposesCanonicalDifficultyValuesInListAndDetails(RecipeDifficulty difficulty) throws Exception {
        RecipeResponse response = recipe(difficulty);
        when(service.listSaved(12)).thenReturn(List.of(response));
        when(service.getSaved(12, ID)).thenReturn(response);

        mockMvc.perform(get(RECIPES).header("Authorization", "Bearer profile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value(difficulty.name()));
        mockMvc.perform(get(RECIPES + "/" + ID).header("Authorization", "Bearer profile-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value(difficulty.name()));
    }

    @Test
    void returnsNotFoundWhenTheRecipeIsUnavailableToTheProfile() throws Exception {
        when(service.getSaved(12, ID)).thenThrow(new NotFoundException("Receita nao encontrada: " + ID));

        mockMvc.perform(get(RECIPES + "/" + ID).header("Authorization", "Bearer profile-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deletesSavedRecipesForTheAuthenticatedProfile() throws Exception {
        mockMvc.perform(delete(RECIPES + "/" + ID).header("Authorization", "Bearer profile-token"))
                .andExpect(status().isNoContent());

        verify(service).deleteSaved(12, ID);
    }

    @Test
    void requiresAuthenticationForListDetailAndDeletion() throws Exception {
        mockMvc.perform(get(RECIPES)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RECIPES + "/" + ID)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(RECIPES + "/" + ID)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    private RecipeResponse recipe() {
        return recipe(RecipeDifficulty.EASY);
    }

    private RecipeResponse recipe(RecipeDifficulty difficulty) {
        return new RecipeResponse(ID, "Frango ao molho", 4, "30 minutos", difficulty,
                List.of(new RecipeIngredientResponse(5, "500g", true, List.of(7), "Peito de frango")),
                "Corte o frango.\nCozinhe por 15 minutos.", "Cerita");
    }
}
