package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.NearbyHouseholdProjection;
import com.example.nexuscore.dto.notification.NearbyStoreCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreOption;
import com.example.nexuscore.dto.notification.NearbyStorePairProjection;
import com.example.nexuscore.dto.notification.NewStoreCandidate;
import com.example.nexuscore.dto.notification.NewStoreProjection;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationCandidateService {

    private static final int MAX_PAGE_SIZE = 500;
    private static final long MAX_WINDOW_DAYS = 31;

    private final PantryItemRepository pantryItemRepository;
    private final ProfileRepository profileRepository;
    private final StoreRepository storeRepository;

    public NotificationCandidateService(PantryItemRepository pantryItemRepository,
                                        ProfileRepository profileRepository,
                                        StoreRepository storeRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.profileRepository = profileRepository;
        this.storeRepository = storeRepository;
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
