package com.example.nexuscore.service;

import com.example.nexuscore.dto.shoppinglist.ShoppingListRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListResponse;
import com.example.nexuscore.model.ShoppingList;
import com.example.nexuscore.repository.ShoppingListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShoppingListServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 12, 10);

    private final ShoppingListRepository repository = mock(ShoppingListRepository.class);
    private final ShoppingListService service = new ShoppingListService(repository, mock(ActivityRecordService.class));

    @BeforeEach
    void setUp() {
        when(repository.save(any(ShoppingList.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void savesProvidedDateForAuthenticatedHousehold() {
        ShoppingListResponse response = service.create(12, new ShoppingListRequest("Compras", DATE));

        ArgumentCaptor<ShoppingList> saved = ArgumentCaptor.forClass(ShoppingList.class);
        verify(repository).save(saved.capture());
        assertEquals(12, saved.getValue().getHouseholdId());
        assertEquals(DATE, saved.getValue().getDate());
        assertEquals(DATE, response.date());
    }

    @Test
    void createsListWithoutDate() {
        ShoppingListResponse response = service.create(12, new ShoppingListRequest("Compras", null));

        assertNull(response.date());
        ArgumentCaptor<ShoppingList> saved = ArgumentCaptor.forClass(ShoppingList.class);
        verify(repository).save(saved.capture());
        assertNull(saved.getValue().getDate());
    }

    @Test
    void updatesDateOfExistingList() {
        ShoppingList list = new ShoppingList(12, "Compras", DATE);
        when(repository.findById("list-id")).thenReturn(Optional.of(list));

        ShoppingListResponse response = service.rename(12, "list-id",
                new ShoppingListRequest("Nova lista", DATE.plusDays(1)));

        verify(repository).save(list);
        assertEquals("Nova lista", response.title());
        assertEquals(DATE.plusDays(1), list.getDate());
        assertEquals(DATE.plusDays(1), response.date());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void preservesExistingDateWhenRenamingWithoutDate(boolean hasDate) {
        LocalDate originalDate = hasDate ? DATE : null;
        ShoppingList list = new ShoppingList(12, "Compras", originalDate);
        when(repository.findById("list-id")).thenReturn(Optional.of(list));

        ShoppingListResponse response = service.rename(12, "list-id", new ShoppingListRequest("Nova lista", null));

        assertEquals("Nova lista", response.title());
        assertEquals(originalDate, list.getDate());
        assertEquals(originalDate, response.date());
        verify(repository).save(list);
    }
}
