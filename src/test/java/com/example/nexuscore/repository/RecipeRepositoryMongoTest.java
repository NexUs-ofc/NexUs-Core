package com.example.nexuscore.repository;

import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.service.ActivityRecordService;
import com.example.nexuscore.service.RecipeService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "mongodb.test.uri", matches = ".+")
class RecipeRepositoryMongoTest {

    private static MongoClient client;
    private static MongoTemplate template;
    private static RecipeRepository recipes;
    private static RecipeAccountRepository recipeAccounts;

    private ActivityRecordService activities;
    private RecipeService service;

    @BeforeAll
    static void connect() {
        client = MongoClients.create(System.getProperty("mongodb.test.uri"));
        template = new MongoTemplate(client, "nexus_recipe_test_" + UUID.randomUUID().toString().replace("-", ""));
        MongoRepositoryFactory factory = new MongoRepositoryFactory(template);
        recipes = factory.getRepository(RecipeRepository.class);
        recipeAccounts = factory.getRepository(RecipeAccountRepository.class);
    }

    @AfterAll
    static void disconnect() {
        try {
            template.getDb().drop();
        } finally {
            client.close();
        }
    }

    @BeforeEach
    void setUp() {
        recipes.deleteAll();
        recipeAccounts.deleteAll();
        activities = mock(ActivityRecordService.class);
        FoodRepository foods = mock(FoodRepository.class);
        Food rice = mock(Food.class);
        when(rice.getId()).thenReturn(41);
        when(rice.getName()).thenReturn("Arroz branco tipo 1");
        when(foods.findAllById(List.of(41))).thenReturn(List.of(rice));
        service = new RecipeService(recipes, activities, foods, recipeAccounts);
    }

    @Test
    void readsStoredMetadataAndIngredientNamesFromAiDocuments() {
        Document document = recipe(1).append("estimated_time", "30 minutos").append("difficulty", "Fácil");
        insert(document);
        link(document.getObjectId("_id"), 12, Instant.parse("2026-10-24T12:00:00Z"));

        RecipeResponse details = service.getSaved(12, document.getObjectId("_id").toHexString());

        assertThat(service.listSaved(12)).containsExactly(details);
        assertThat(details.servingSize()).isEqualTo(4);
        assertThat(details.estimatedTime()).isEqualTo("30 minutos");
        assertThat(details.difficulty()).isEqualTo("Fácil");
        assertThat(details.ingredients().get(0).foodName()).isEqualTo("Arroz branco tipo 1");
        assertThat(details.ingredients().get(0).requiredQuantity()).isEqualTo("200g");
        assertThat(details.instructions()).isEqualTo("Lave o arroz.\nCozinhe até ficar macio.");
    }

    @Test
    void readsLegacyDocumentsWithoutOptionalMetadata() {
        Document document = recipe(1).append("account_id", 12).append("created_at", Date.from(Instant.EPOCH));
        insert(document);

        RecipeResponse details = service.getSaved(12, document.getObjectId("_id").toHexString());

        assertThat(service.listSaved(12)).containsExactly(details);
        assertThat(details.servingSize()).isEqualTo(4);
        assertThat(details.estimatedTime()).isNull();
        assertThat(details.difficulty()).isNull();
    }

    @Test
    void mergesLegacyAndLinkedRecipesWithoutDuplicatesOrOtherProfilesRecipes() {
        Document legacy = recipe(1).append("account_id", 12).append("created_at", Date.from(Instant.EPOCH));
        Document linked = recipe(2).append("account_id", 12).append("created_at", Date.from(Instant.EPOCH));
        Document undated = recipe(3);
        Document foreign = recipe(4).append("account_id", 13);
        insert(legacy);
        insert(linked);
        insert(undated);
        insert(foreign);
        link(linked.getObjectId("_id"), 12, Instant.EPOCH.plusSeconds(60));
        link(linked.getObjectId("_id"), 12, Instant.EPOCH.minusSeconds(60));
        link(undated.getObjectId("_id"), 12, null);
        link(foreign.getObjectId("_id"), 13, Instant.EPOCH.plusSeconds(120));
        link(new ObjectId(), 12, Instant.EPOCH.plusSeconds(180));

        assertThat(service.listSaved(12)).extracting(RecipeResponse::id).containsExactly(
                linked.getObjectId("_id").toHexString(), legacy.getObjectId("_id").toHexString(),
                undated.getObjectId("_id").toHexString());
    }

    @Test
    void cannotReadOrRemoveAnotherProfilesRecipes() {
        Document foreign = recipe(1).append("account_id", 13);
        insert(foreign);
        ObjectId id = foreign.getObjectId("_id");
        link(id, 13, Instant.EPOCH);

        assertThat(service.listSaved(12)).isEmpty();
        assertThatThrownBy(() -> service.getSaved(12, id.toHexString())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deleteSaved(12, id.toHexString())).isInstanceOf(NotFoundException.class);
        assertThat(recipeAccounts.existsByRecipeIdAndAccountId(id, 13)).isTrue();
        assertThat(recipes.findByIdAndAccountId(id, 13)).isPresent();
        verifyNoInteractions(activities);
    }

    @Test
    void removingSharedRecipeKeepsOtherProfilesAndUnknownFields() {
        Document document = recipe(1).append("is_liked", true).append("additional_field", "preservado");
        insert(document);
        ObjectId id = document.getObjectId("_id");
        link(id, 12, Instant.EPOCH);
        link(id, 12, Instant.EPOCH.plusSeconds(60));
        link(id, 13, Instant.EPOCH);

        service.deleteSaved(12, id.toHexString());

        assertThat(service.listSaved(12)).isEmpty();
        assertThatThrownBy(() -> service.getSaved(12, id.toHexString())).isInstanceOf(NotFoundException.class);
        assertThat(recipeAccounts.existsByRecipeIdAndAccountId(id, 12)).isFalse();
        assertThat(recipeAccounts.existsByRecipeIdAndAccountId(id, 13)).isTrue();
        assertThat(service.getSaved(13, id.toHexString()).id()).isEqualTo(id.toHexString());
        assertThat(template.getCollection("recipes").find(new Document("_id", id)).first())
                .containsEntry("is_liked", true).containsEntry("additional_field", "preservado");
        verify(activities).record(12, "saved_recipe_removed", "Removeu a receita salva Arroz 1");
    }

    @Test
    void removesBothLegacyAndLinkedOwnershipForTheSameProfile() {
        Document document = recipe(1).append("account_id", 12).append("is_liked", true)
                .append("estimated_time", "1 hora").append("difficulty", "Média");
        insert(document);
        ObjectId id = document.getObjectId("_id");
        link(id, 12, Instant.EPOCH);
        link(id, 13, Instant.EPOCH);

        service.deleteSaved(12, id.toHexString());

        assertThat(service.listSaved(12)).isEmpty();
        assertThat(recipeAccounts.existsByRecipeIdAndAccountId(id, 12)).isFalse();
        assertThat(recipes.findByIdAndAccountId(id, 12)).isEmpty();
        assertThat(service.getSaved(13, id.toHexString()).estimatedTime()).isEqualTo("1 hora");
        assertThat(template.getCollection("recipes").find(new Document("_id", id)).first())
                .doesNotContainKey("account_id").containsEntry("is_liked", true).containsEntry("difficulty", "Média");
    }

    @Test
    void removingALinkDoesNotRemoveAnotherProfilesLegacyOwnership() {
        Document document = recipe(1).append("account_id", 13);
        insert(document);
        ObjectId id = document.getObjectId("_id");
        link(id, 12, Instant.EPOCH);

        service.deleteSaved(12, id.toHexString());

        assertThat(service.listSaved(12)).isEmpty();
        assertThat(recipes.findByIdAndAccountId(id, 13)).isPresent();
        assertThat(service.getSaved(13, id.toHexString()).id()).isEqualTo(id.toHexString());
    }

    @Test
    void removingALegacyRecipePreservesItsDocument() {
        Document document = recipe(1).append("account_id", 12).append("additional_field", "preservado");
        insert(document);
        ObjectId id = document.getObjectId("_id");

        service.deleteSaved(12, id.toHexString());

        assertThat(service.listSaved(12)).isEmpty();
        assertThatThrownBy(() -> service.getSaved(12, id.toHexString())).isInstanceOf(NotFoundException.class);
        Recipe recipe = recipes.findById(id).orElseThrow();
        assertThat(recipe.getAccountId()).isNull();
        assertThat(recipe.getInstructions()).isEqualTo(document.getString("instructions"));
        assertThat(template.getCollection("recipes").find(new Document("_id", id)).first())
                .containsEntry("additional_field", "preservado");
    }

    private Document recipe(int id) {
        return new Document("_id", new ObjectId(String.format("%024x", id)))
                .append("title", "Arroz " + id)
                .append("serving_size", 4)
                .append("ingredients", List.of(new Document("food_id", 41).append("required_quantity", "200g")
                        .append("mandatory", true).append("possible_substitutes", List.of())))
                .append("instructions", "Lave o arroz.\nCozinhe até ficar macio.")
                .append("created_by", "ai");
    }

    private void insert(Document document) {
        template.getCollection("recipes").insertOne(document);
    }

    private void link(ObjectId recipeId, int accountId, Instant createdAt) {
        Document link = new Document("id_recipe", recipeId).append("id_account", accountId);
        if (createdAt != null) {
            link.append("created_at", Date.from(createdAt));
        }
        template.getCollection("recipe_accounts").insertOne(link);
    }
}
