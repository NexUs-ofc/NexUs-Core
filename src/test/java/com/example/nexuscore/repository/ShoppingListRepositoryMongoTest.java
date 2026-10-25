package com.example.nexuscore.repository;

import com.example.nexuscore.model.ShoppingList;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "mongodb.test.uri", matches = ".+")
class ShoppingListRepositoryMongoTest {

    private static MongoClient client;
    private static MongoTemplate template;
    private static ShoppingListRepository repository;

    @BeforeAll
    static void connect() {
        client = MongoClients.create(System.getProperty("mongodb.test.uri"));
        String database = "nexus_shopping_list_test_" + UUID.randomUUID().toString().replace("-", "");
        template = new MongoTemplate(client, database);
        repository = new MongoRepositoryFactory(template).getRepository(ShoppingListRepository.class);
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
    void clearLists() {
        repository.deleteAll();
    }

    @Test
    void storesDocumentWithoutDateWhenNotProvided() {
        ShoppingList saved = repository.save(new ShoppingList(12, "Compras", null));

        Document document = template.getCollection("shopping_list").find(new Document("household_id", 12)).first();

        assertThat(document).isNotNull().doesNotContainKeys("date", "event_id");
        assertThat(repository.findById(saved.getId()).orElseThrow().getDate()).isNull();
    }

    @Test
    void persistsAndUpdatesProvidedDate() {
        LocalDate date = LocalDate.of(2026, 12, 10);
        ShoppingList saved = repository.save(new ShoppingList(12, "Compras", date));

        Document document = template.getCollection("shopping_list").find(new Document("household_id", 12)).first();
        assertThat(document).isNotNull();
        assertThat(document.get("date")).isInstanceOf(Date.class);
        assertThat(repository.findById(saved.getId()).orElseThrow().getDate()).isEqualTo(date);

        saved.setDate(date.plusDays(1));
        repository.save(saved);

        assertThat(repository.findById(saved.getId()).orElseThrow().getDate()).isEqualTo(date.plusDays(1));
    }

    @Test
    void preservesAbsentDateWhenUpdatingTitle() {
        template.getCollection("shopping_list").insertOne(new Document("_id", "undated-list")
                .append("household_id", 12).append("title", "Compras"));

        ShoppingList list = repository.findById("undated-list").orElseThrow();
        assertThat(list.getDate()).isNull();
        list.setTitle("Nova lista");
        repository.save(list);

        Document document = template.getCollection("shopping_list").find(new Document("_id", "undated-list")).first();
        assertThat(document).isNotNull().doesNotContainKeys("date", "event_id");
        assertThat(document.getString("title")).isEqualTo("Nova lista");
    }
}
