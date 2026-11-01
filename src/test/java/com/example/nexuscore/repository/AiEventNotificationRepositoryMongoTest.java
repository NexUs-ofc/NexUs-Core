package com.example.nexuscore.repository;

import com.example.nexuscore.model.AiEventNotificationSource;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.bson.types.ObjectId;
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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "mongodb.test.uri", matches = ".+")
class AiEventNotificationRepositoryMongoTest {

    private static MongoClient client;
    private static MongoTemplate template;
    private static AiEventNotificationRepository repository;

    @BeforeAll
    static void connect() {
        client = MongoClients.create(System.getProperty("mongodb.test.uri"));
        template = new MongoTemplate(client, "nexus_ai_events_test_" + UUID.randomUUID().toString().replace("-", ""));
        repository = new MongoRepositoryFactory(template).getRepository(AiEventNotificationRepository.class);
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
    void clearEvents() {
        template.getCollection("events").deleteMany(new Document());
    }

    @Test
    void readsPythonDocumentsAtDayBoundariesWithStablePaginationAndObjectIdConversion() {
        Instant from = Instant.parse("2026-10-31T00:00:00Z");
        Instant until = Instant.parse("2026-11-01T00:00:00Z");
        ObjectId first = new ObjectId("507f1f77bcf86cd799439011");
        ObjectId second = new ObjectId("507f1f77bcf86cd799439012");
        ObjectId last = new ObjectId("507f1f77bcf86cd799439013");
        template.getCollection("events").insertMany(List.of(
                document(new ObjectId(), from.minusMillis(1)),
                document(second, from), document(first, from), document(last, until.minusMillis(1)),
                document(new ObjectId(), until), new Document("title", "Sem data"),
                new Document("title", "Data nula").append("date", null)));

        Slice<AiEventNotificationSource> page = repository.findNotificationCandidates(from, until,
                PageRequest.of(0, 2, Sort.by("date", "id").ascending()));
        Slice<AiEventNotificationSource> next = repository.findNotificationCandidates(from, until,
                PageRequest.of(1, 2, Sort.by("date", "id").ascending()));

        assertThat(page.getContent()).extracting(AiEventNotificationSource::getId)
                .containsExactly(first.toHexString(), second.toHexString());
        assertThat(page.hasNext()).isTrue();
        assertThat(next.getContent()).extracting(AiEventNotificationSource::getId)
                .containsExactly(last.toHexString());
        assertThat(next.hasNext()).isFalse();
        assertThat(page.getContent().get(0).getDate()).isEqualTo(from);
        assertThat(next.getContent().get(0).getDate()).isEqualTo(until.minusMillis(1));
        assertThat(page.getContent().get(0).getHouseholdId()).isEqualTo(12);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Jantar");
        assertThat(template.getCollection("events").countDocuments()).isEqualTo(7);
        assertThat(template.getCollection("events").find(new Document("_id", first)).first())
                .containsKey("recipes");
    }

    private Document document(ObjectId id, Instant date) {
        return new Document("_id", id).append("household_id", 12).append("title", "Jantar")
                .append("description", "Jantar em familia").append("date", Date.from(date))
                .append("duration", 60).append("local", "Casa").append("qtd_people", 4)
                .append("recipes", List.of());
    }
}
