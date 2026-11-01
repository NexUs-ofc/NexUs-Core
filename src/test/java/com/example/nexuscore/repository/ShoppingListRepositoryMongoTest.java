package com.example.nexuscore.repository;

import com.example.nexuscore.model.ShoppingList;
import com.example.nexuscore.model.ShoppingListItem;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
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
    void selectsOnlyDatedListsInInclusiveWindowAndPreservesCompletionWithStablePagination() {
        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = from.plusDays(7);
        repository.save(new ShoppingList(12, "Sem data", null));
        repository.save(new ShoppingList(12, "Antes", from.minusDays(1)));
        repository.save(new ShoppingList(12, "Depois", to.plusDays(1)));
        ShoppingList first = new ShoppingList(12, "Completa", from);
        first.getArrayList().add(new ShoppingListItem(1, "Leite", 1.0, "UN", true));
        first.getArrayList().add(new ShoppingListItem(2, "Arroz", 2.0, "UN", true));
        first = repository.save(first);
        ShoppingList second = new ShoppingList(12, "Parcial", from);
        second.getArrayList().add(new ShoppingListItem(1, "Leite", 1.0, "UN", true));
        second.getArrayList().add(new ShoppingListItem(2, "Arroz", 2.0, "UN", false));
        second = repository.save(second);
        ShoppingList last = repository.save(new ShoppingList(12, "No limite", to));

        Slice<ShoppingList> page = repository.findNotificationCandidates(from, to,
                PageRequest.of(0, 2, Sort.by("date", "id").ascending()));
        Slice<ShoppingList> next = repository.findNotificationCandidates(from, to,
                PageRequest.of(1, 2, Sort.by("date", "id").ascending()));

        assertThat(page.getContent()).extracting(ShoppingList::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(page.hasNext()).isTrue();
        assertThat(next.getContent()).extracting(ShoppingList::getId).containsExactly(last.getId());
        assertThat(next.hasNext()).isFalse();
        assertThat(page.getContent().get(0).getArrayList()).extracting(ShoppingListItem::isChecked)
                .containsExactly(true, true);
        assertThat(page.getContent().get(1).getArrayList()).extracting(ShoppingListItem::isChecked)
                .containsExactly(true, false);
        assertThat(page.getContent().get(0).getHouseholdId()).isEqualTo(12);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Completa");
        assertThat(next.getContent().get(0).getDate()).isEqualTo(to);
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
