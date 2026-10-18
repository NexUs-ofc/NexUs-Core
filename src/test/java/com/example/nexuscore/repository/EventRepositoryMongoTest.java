package com.example.nexuscore.repository;

import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.model.Event;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.service.NotificationCandidateService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = "mongodb.test.uri", matches = ".+")
class EventRepositoryMongoTest {

    private static MongoClient client;
    private static MongoTemplate template;
    private static EventRepository repository;

    @BeforeAll
    static void connect() {
        client = MongoClients.create(System.getProperty("mongodb.test.uri"));
        template = new MongoTemplate(client, "nexus_event_test_" + UUID.randomUUID().toString().replace("-", ""));
        repository = new MongoRepositoryFactory(template).getRepository(EventRepository.class);
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
        repository.deleteAll();
    }

    @Test
    void includesStartAndLastMillisecondButExcludesNextMidnight() {
        LocalDateTime from = LocalDate.of(2026, 10, 18).atStartOfDay();
        LocalDateTime until = from.plusDays(1);
        insert(1, from.minusNanos(1_000_000));
        Event start = insert(2, from);
        Event last = insert(3, until.minusNanos(1_000_000));
        insert(4, until);
        insert(5, until.plusNanos(1_000_000));

        Slice<Event> result = repository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAscIdAsc(
                from, until, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Event::getId).containsExactly(start.getId(), last.getId());
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void sortsByDateThenIdAcrossPagesWithoutLosingTies() {
        LocalDateTime from = LocalDate.of(2026, 10, 18).atStartOfDay();
        Event last = insert(1, from.plusHours(2));
        Event third = insert(4, from.plusHours(1));
        Event second = insert(3, from.plusHours(1));
        Event first = insert(2, from.plusHours(1));

        Slice<Event> page0 = repository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAscIdAsc(
                from, from.plusDays(1), PageRequest.of(0, 2));
        Slice<Event> page1 = repository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAscIdAsc(
                from, from.plusDays(1), PageRequest.of(1, 2));
        Slice<Event> page2 = repository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAscIdAsc(
                from, from.plusDays(1), PageRequest.of(2, 2));

        assertThat(page0.getContent()).extracting(Event::getId).containsExactly(first.getId(), second.getId());
        assertThat(page0.hasNext()).isTrue();
        assertThat(page1.getContent()).extracting(Event::getId).containsExactly(third.getId(), last.getId());
        assertThat(page1.hasNext()).isFalse();
        assertThat(page2.getContent()).isEmpty();
        assertThat(page2.hasNext()).isFalse();
    }

    @Test
    void serviceKeepsInclusiveEndDateAndPaginationWithRealMongo() {
        LocalDate from = LocalDate.of(2026, 10, 18);
        LocalDate to = from.plusDays(2);
        insert(1, from.atStartOfDay().minusNanos(1_000_000));
        Event first = insert(2, from.atStartOfDay());
        Event last = insert(3, to.plusDays(1).atStartOfDay().minusNanos(1_000_000));
        insert(4, to.plusDays(1).atStartOfDay());
        ProfileRepository profiles = mock(ProfileRepository.class);
        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(12);
        when(profiles.findByIdInAndStatus(Set.of(12), ProfileStatus.ACTIVE)).thenReturn(List.of(profile));
        NotificationCandidateService service = new NotificationCandidateService(
                mock(PantryItemRepository.class), repository, profiles, mock(StoreRepository.class));

        NotificationCandidatePage<EventNotificationCandidate> page0 = service.events(from, to, 0, 1);
        NotificationCandidatePage<EventNotificationCandidate> page1 = service.events(from, to, 1, 1);

        assertThat(page0.content()).extracting(EventNotificationCandidate::eventId)
                .containsExactly(first.getId().toHexString());
        assertThat(page0.hasNext()).isTrue();
        assertThat(page1.content()).extracting(EventNotificationCandidate::eventId)
                .containsExactly(last.getId().toHexString());
        assertThat(page1.hasNext()).isFalse();
    }

    private Event insert(int id, LocalDateTime date) {
        Event event = new Event(12, "Event " + id, "", date, 60, "Home", 2);
        ReflectionTestUtils.setField(event, "id", new ObjectId(String.format("%024x", id)));
        return repository.save(event);
    }
}
