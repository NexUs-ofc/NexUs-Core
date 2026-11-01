package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.model.AiEventNotificationSource;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.AiEventNotificationRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.ShoppingListRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventNotificationCandidateServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 31);
    private static final Instant FROM = Instant.parse("2026-10-31T00:00:00Z");
    private static final Instant UNTIL = Instant.parse("2026-11-01T00:00:00Z");
    private final AiEventNotificationRepository events = mock(AiEventNotificationRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final NotificationCandidateService service = new NotificationCandidateService(
            mock(PantryItemRepository.class), profiles, mock(StoreRepository.class),
            mock(ShoppingListRepository.class), events);

    @Test
    void preservesTheAiWallClockAndOnlyIncludesActiveHouseholdOwners() {
        PageRequest page = PageRequest.of(0, 10, Sort.by("date", "id").ascending());
        when(events.findNotificationCandidates(FROM, UNTIL, page)).thenReturn(new SliceImpl<>(List.of(
                event("active", 12), event("inactive-or-missing", 13), event("company", 14), event("orphan", null)),
                page, true));
        when(profiles.findByIdInAndStatus(List.of(12, 13, 14), ProfileStatus.ACTIVE))
                .thenReturn(List.of(profile(12, ProfileType.HOUSEHOLD), profile(14, ProfileType.COMPANY)));

        NotificationCandidatePage<EventNotificationCandidate> result = service.events(DATE, DATE, 0, 10);

        assertThat(result.content()).containsExactly(new EventNotificationCandidate(
                12, "active", "Jantar", LocalDateTime.of(2026, 10, 31, 0, 30)));
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void preservesPaginationWhenAllEventsOnAPageAreFilteredOut() {
        PageRequest page = PageRequest.of(1, 1, Sort.by("date", "id").ascending());
        when(events.findNotificationCandidates(FROM, UNTIL, page)).thenReturn(new SliceImpl<>(
                List.of(event("orphan", null)), page, true));
        NotificationCandidatePage<EventNotificationCandidate> result = service.events(DATE, DATE, 1, 1);
        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isTrue();
        verifyNoInteractions(profiles);
    }

    @Test
    void validatesTheWindowAndPaginationBeforeReadingEvents() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(null, DATE, 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(DATE, null, 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(DATE.plusDays(1), DATE, 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(DATE, DATE.plusDays(32), 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(DATE, DATE, -1, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(DATE, DATE, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(DATE, DATE, 0, 501));
        assertThatIllegalArgumentException().isThrownBy(() -> service.events(LocalDate.MAX, LocalDate.MAX, 0, 100));
        verifyNoInteractions(events, profiles);
    }

    private AiEventNotificationSource event(String id, Integer owner) {
        AiEventNotificationSource event = BeanUtils.instantiateClass(AiEventNotificationSource.class);
        ReflectionTestUtils.setField(event, "id", id);
        ReflectionTestUtils.setField(event, "householdId", owner);
        ReflectionTestUtils.setField(event, "title", "Jantar");
        ReflectionTestUtils.setField(event, "date", Instant.parse("2026-10-31T00:30:00Z"));
        return event;
    }

    private Profile profile(int id, ProfileType type) {
        Profile profile = BeanUtils.instantiateClass(Profile.class);
        ReflectionTestUtils.setField(profile, "id", id);
        ReflectionTestUtils.setField(profile, "type", type);
        return profile;
    }
}
