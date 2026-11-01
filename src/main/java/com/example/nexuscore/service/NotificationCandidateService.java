package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NearbyHouseholdProjection;
import com.example.nexuscore.dto.notification.NearbyStoreCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreOption;
import com.example.nexuscore.dto.notification.NearbyStorePairProjection;
import com.example.nexuscore.dto.notification.NewStoreCandidate;
import com.example.nexuscore.dto.notification.NewStoreProjection;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.dto.notification.ShoppingListNotificationCandidate;
import com.example.nexuscore.model.AiEventNotificationSource;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.model.ShoppingList;
import com.example.nexuscore.model.ShoppingListItem;
import com.example.nexuscore.repository.AiEventNotificationRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.ShoppingListRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationCandidateService {

    private static final int MAX_PAGE_SIZE = 500;
    private static final long MAX_WINDOW_DAYS = 31;

    private final PantryItemRepository pantryItemRepository;
    private final ProfileRepository profileRepository;
    private final StoreRepository storeRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final AiEventNotificationRepository eventRepository;

    public NotificationCandidateService(PantryItemRepository pantryItemRepository,
                                        ProfileRepository profileRepository,
                                        StoreRepository storeRepository,
                                        ShoppingListRepository shoppingListRepository,
                                        AiEventNotificationRepository eventRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.profileRepository = profileRepository;
        this.storeRepository = storeRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.eventRepository = eventRepository;
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
        Slice<AiEventNotificationSource> events = eventRepository.findNotificationCandidates(
                from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                PageRequest.of(page, size, Sort.by("date", "id").ascending()));
        List<Integer> profileIds = events.getContent().stream().map(AiEventNotificationSource::getHouseholdId)
                .filter(Objects::nonNull).distinct().toList();
        Set<Integer> activeHouseholdIds = activeHouseholdIds(profileIds);
        List<EventNotificationCandidate> content = events.getContent().stream()
                .filter(event -> event.getHouseholdId() != null
                        && activeHouseholdIds.contains(event.getHouseholdId()))
                .map(event -> new EventNotificationCandidate(event.getHouseholdId(), event.getId(), event.getTitle(),
                        LocalDateTime.ofInstant(event.getDate(), ZoneOffset.UTC)))
                .toList();
        return new NotificationCandidatePage<>(content, events.hasNext());
    }

    public NotificationCandidatePage<ShoppingListNotificationCandidate> shoppingLists(
            LocalDate from, LocalDate to, int page, int size) {
        validateRequest(from, to, page, size);
        Slice<ShoppingList> lists = shoppingListRepository.findNotificationCandidates(from, to,
                PageRequest.of(page, size, Sort.by("date", "id").ascending()));
        List<Integer> profileIds = lists.getContent().stream().map(ShoppingList::getHouseholdId)
                .filter(Objects::nonNull).distinct().toList();
        Set<Integer> activeHouseholdIds = activeHouseholdIds(profileIds);
        List<ShoppingListNotificationCandidate> content = lists.getContent().stream()
                .filter(list -> list.getHouseholdId() != null
                        && activeHouseholdIds.contains(list.getHouseholdId()))
                .map(list -> new ShoppingListNotificationCandidate(list.getHouseholdId(), list.getId(),
                        list.getTitle(), list.getDate(), !list.getArrayList().isEmpty()
                                && list.getArrayList().stream().allMatch(ShoppingListItem::isChecked)))
                .toList();
        return new NotificationCandidatePage<>(content, lists.hasNext());
    }

    public NotificationCandidatePage<NearbyStoreCandidate> nearbyStores(double radiusKm, int page, int size) {
        validatePage(page, size);
        validateRadius(radiusKm);
        Slice<Profile> profiles = profileRepository.findActiveGeocodedHouseholds(
                PageRequest.of(page, size, Sort.by("id").ascending()));
        List<Integer> profileIds = profiles.getContent().stream().map(Profile::getId).toList();
        Map<Integer, List<NearbyStoreOption>> storesByProfile = new LinkedHashMap<>();
        if (!profileIds.isEmpty()) {
            for (NearbyStorePairProjection pair : storeRepository.findNearbyStoresForProfiles(profileIds, radiusKm)) {
                storesByProfile.computeIfAbsent(pair.getProfileId(), ignored -> new ArrayList<>())
                        .add(new NearbyStoreOption(pair.getStoreId(), pair.getStoreName(), pair.getDistanceKm()));
            }
        }
        List<NearbyStoreCandidate> content = profiles.getContent().stream()
                .filter(profile -> storesByProfile.containsKey(profile.getId()))
                .map(profile -> new NearbyStoreCandidate(profile.getId(), storesByProfile.get(profile.getId())))
                .toList();
        return new NotificationCandidatePage<>(content, profiles.hasNext());
    }

    public NotificationCandidatePage<NewStoreCandidate> newStores(
            LocalDate from, LocalDate to, double radiusKm, int page, int size) {
        validateRequest(from, to, page, size);
        validateRadius(radiusKm);
        Slice<NewStoreProjection> stores = storeRepository.findNewActiveStores(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay(), PageRequest.of(page, size));
        List<NewStoreCandidate> content = stores.getContent().stream().map(store -> {
            List<Integer> profileIds = storeRepository.findActiveHouseholdsNearStore(store.getStoreId(), radiusKm)
                    .stream().map(NearbyHouseholdProjection::getProfileId).toList();
            return new NewStoreCandidate(store.getStoreId(), store.getStoreName(),
                    store.getCreatedAt().toLocalDate(), profileIds);
        }).toList();
        return new NotificationCandidatePage<>(content, stores.hasNext());
    }

    private Set<Integer> activeHouseholdIds(List<Integer> profileIds) {
        return profileIds.isEmpty() ? Set.of()
                : profileRepository.findByIdInAndStatus(profileIds, ProfileStatus.ACTIVE).stream()
                        .filter(profile -> profile.getType() == ProfileType.HOUSEHOLD)
                        .map(Profile::getId).collect(Collectors.toSet());
    }

    private void validateRequest(LocalDate from, LocalDate to, int page, int size) {
        if (from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > MAX_WINDOW_DAYS
                || to.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("Intervalo de datas inválido para notificações");
        }
        validatePage(page, size);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Paginação inválida para notificações");
        }
    }

    private void validateRadius(double radiusKm) {
        if (!Double.isFinite(radiusKm) || radiusKm <= 0 || radiusKm > 100) {
            throw new IllegalArgumentException("Raio deve ser maior que zero e menor ou igual a 100 km");
        }
    }
}
