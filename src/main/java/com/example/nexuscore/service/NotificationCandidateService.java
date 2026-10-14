package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.model.Event;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.repository.EventRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationCandidateService {

    private static final int MAX_PAGE_SIZE = 500;
    private static final long MAX_WINDOW_DAYS = 31;

    private final PantryItemRepository pantryItemRepository;
    private final EventRepository eventRepository;
    private final ProfileRepository profileRepository;

    public NotificationCandidateService(PantryItemRepository pantryItemRepository,
                                        EventRepository eventRepository,
                                        ProfileRepository profileRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.eventRepository = eventRepository;
        this.profileRepository = profileRepository;
    }

    public NotificationCandidatePage<ProductNotificationCandidate> products(
            LocalDate from, LocalDate to, int page, int size) {
        validateRequest(from, to, page, size);
        Slice<ProductNotificationCandidate> candidates = pantryItemRepository
                .findExpiringProducts(from, to, PageRequest.of(page, size))
                .map(item -> new ProductNotificationCandidate(
                        item.getProfileId(), item.getItemId(), item.getFoodName(), item.getExpiryDate()));
        return new NotificationCandidatePage<>(candidates.getContent(), candidates.hasNext());
    }

    public NotificationCandidatePage<EventNotificationCandidate> events(
            LocalDate from, LocalDate to, int page, int size) {
        validateRequest(from, to, page, size);
        Slice<Event> events = eventRepository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAscIdAsc(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), PageRequest.of(page, size));
        Set<Integer> householdIds = events.getContent().stream()
                .map(Event::getHouseholdId)
                .collect(Collectors.toSet());
        Set<Integer> activeProfileIds = householdIds.isEmpty()
                ? Set.of()
                : profileRepository.findByIdInAndStatus(householdIds, ProfileStatus.ACTIVE).stream()
                        .map(profile -> profile.getId())
                        .collect(Collectors.toCollection(HashSet::new));
        List<EventNotificationCandidate> content = events.getContent().stream()
                .filter(event -> activeProfileIds.contains(event.getHouseholdId()))
                .map(event -> new EventNotificationCandidate(
                        event.getHouseholdId(), event.getId().toHexString(), event.getTitle(), event.getDate()))
                .toList();
        return new NotificationCandidatePage<>(content, events.hasNext());
    }

    private void validateRequest(LocalDate from, LocalDate to, int page, int size) {
        if (from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > MAX_WINDOW_DAYS
                || to.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("Intervalo de datas inválido para notificações");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Paginação inválida para notificações");
        }
    }
}
