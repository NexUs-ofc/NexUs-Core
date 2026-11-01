package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ShoppingListNotificationCandidate;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.model.ShoppingList;
import com.example.nexuscore.model.ShoppingListItem;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShoppingListNotificationCandidateServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 31);
    private static final LocalDate TO = FROM.plusDays(7);
    private final ShoppingListRepository lists = mock(ShoppingListRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final PantryItemRepository pantry = mock(PantryItemRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final NotificationCandidateService service =
            new NotificationCandidateService(pantry, profiles, stores, lists);

    @Test
    void includesCompletePartialUncheckedAndEmptyListsWithTheirOwnTargetIds() {
        PageRequest page = page(0, 10);
        when(lists.findNotificationCandidates(FROM, TO, page)).thenReturn(new SliceImpl<>(List.of(
                list("complete", 12, true, true), list("partial", 12, true, false),
                list("unchecked", 12, false), list("empty", 12)), page, false));
        when(profiles.findByIdInAndStatus(List.of(12), ProfileStatus.ACTIVE))
                .thenReturn(List.of(profile(12, ProfileType.HOUSEHOLD)));

        NotificationCandidatePage<ShoppingListNotificationCandidate> result = service.shoppingLists(FROM, TO, 0, 10);

        assertThat(result.content()).extracting(ShoppingListNotificationCandidate::shoppingListId)
                .containsExactly("complete", "partial", "unchecked", "empty");
        assertThat(result.content()).extracting(ShoppingListNotificationCandidate::completed)
                .containsExactly(true, false, false, false);
        assertThat(result.content()).allSatisfy(candidate -> {
            assertThat(candidate.profileId()).isEqualTo(12);
            assertThat(candidate.title()).isEqualTo("Mercado");
            assertThat(candidate.date()).isEqualTo(FROM);
        });
        assertThat(result.hasNext()).isFalse();
        verifyNoInteractions(pantry, stores);
    }

    @Test
    void filtersInactiveMissingAndNonHouseholdProfilesWithoutEndingPaginationEarly() {
        PageRequest page = page(0, 2);
        when(lists.findNotificationCandidates(FROM, TO, page)).thenReturn(new SliceImpl<>(
                List.of(list("inactive-or-missing", 13), list("company", 14)), page, true));
        when(profiles.findByIdInAndStatus(List.of(13, 14), ProfileStatus.ACTIVE))
                .thenReturn(List.of(profile(14, ProfileType.COMPANY)));
        PageRequest next = page(1, 2);
        when(lists.findNotificationCandidates(FROM, TO, next)).thenReturn(new SliceImpl<>(
                List.of(list("active", 12, true)), next, false));
        when(profiles.findByIdInAndStatus(List.of(12), ProfileStatus.ACTIVE))
                .thenReturn(List.of(profile(12, ProfileType.HOUSEHOLD)));

        NotificationCandidatePage<ShoppingListNotificationCandidate> first = service.shoppingLists(FROM, TO, 0, 2);
        NotificationCandidatePage<ShoppingListNotificationCandidate> second = service.shoppingLists(FROM, TO, 1, 2);

        assertThat(first.content()).isEmpty();
        assertThat(first.hasNext()).isTrue();
        assertThat(second.content()).extracting(ShoppingListNotificationCandidate::shoppingListId)
                .containsExactly("active");
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void skipsProfileLookupForAnEmptyPageOrMissingOwner() {
        PageRequest page = page(0, 10);
        when(lists.findNotificationCandidates(FROM, TO, page)).thenReturn(new SliceImpl<>(List.of(), page, false));
        assertThat(service.shoppingLists(FROM, TO, 0, 10).content()).isEmpty();
        when(lists.findNotificationCandidates(FROM, TO, page)).thenReturn(new SliceImpl<>(
                List.of(list("orphan", null)), page, false));
        assertThat(service.shoppingLists(FROM, TO, 0, 10).content()).isEmpty();
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsInvalidWindowsAndPaginationBeforeAccessingData() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.shoppingLists(null, TO, 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.shoppingLists(FROM, null, 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.shoppingLists(TO, FROM, 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.shoppingLists(FROM, FROM.plusDays(32), 0, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.shoppingLists(FROM, TO, -1, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> service.shoppingLists(FROM, TO, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> service.shoppingLists(FROM, TO, 0, 501));
        assertThatIllegalArgumentException().isThrownBy(() ->
                service.shoppingLists(LocalDate.MAX, LocalDate.MAX, 0, 100));
        verifyNoInteractions(lists, profiles);
    }

    private PageRequest page(int number, int size) {
        return PageRequest.of(number, size, Sort.by("date", "id").ascending());
    }

    private ShoppingList list(String id, Integer owner, boolean... checkedItems) {
        ShoppingList list = new ShoppingList(owner, "Mercado", FROM);
        ReflectionTestUtils.setField(list, "id", id);
        for (boolean checked : checkedItems) {
            list.getArrayList().add(new ShoppingListItem(1, "Leite", 1.0, "UN", checked));
        }
        return list;
    }

    private Profile profile(int id, ProfileType type) {
        Profile profile = BeanUtils.instantiateClass(Profile.class);
        ReflectionTestUtils.setField(profile, "id", id);
        ReflectionTestUtils.setField(profile, "type", type);
        return profile;
    }
}
