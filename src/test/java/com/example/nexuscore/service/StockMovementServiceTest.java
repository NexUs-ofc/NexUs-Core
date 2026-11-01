package com.example.nexuscore.service;

import com.example.nexuscore.dto.stock.StockMovementRequest;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.StockMovement;
import com.example.nexuscore.model.StockMovementType;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockMovementServiceTest {

    private final PantryItemRepository items = mock(PantryItemRepository.class);
    private final StockMovementRepository movements = mock(StockMovementRepository.class);
    private final StockMovementService service = new StockMovementService(items, movements);
    private PantryItem item;

    @BeforeEach
    void setUp() {
        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(12);
        item = new PantryItem(null, profile, 5, null);
        ReflectionTestUtils.setField(item, "id", 7);
        when(items.findByIdForUpdate(7)).thenReturn(Optional.of(item));
        when(movements.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            ReflectionTestUtils.setField(movement, "id", 20);
            return movement;
        });
    }

    @ParameterizedTest
    @EnumSource(StockMovementType.class)
    void recordsPartialMovementAndKeepsRemainingStock(StockMovementType type) {
        var response = service.register(12, 7, new StockMovementRequest(type, 2));

        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(response.id()).isEqualTo(20);
        assertThat(response.pantryItemId()).isEqualTo(7);
        assertThat(response.type()).isEqualTo(type);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.remainingQuantity()).isEqualTo(3);
        assertThat(response.eventDate()).isNotNull();
        ArgumentCaptor<StockMovement> saved = ArgumentCaptor.forClass(StockMovement.class);
        verify(movements).save(saved.capture());
        assertThat(saved.getValue().getPantryItem()).isSameAs(item);
        verify(items).findByIdForUpdate(7);
        verify(items).save(item);
        verify(items, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(StockMovementType.class)
    void keepsItemWithZeroStockAfterFullMovement(StockMovementType type) {
        assertThat(service.register(12, 7, new StockMovementRequest(type, 5)).remainingQuantity()).isZero();
        assertThat(item.getQuantity()).isZero();
        verify(items).save(item);
        verify(items, never()).delete(any());
    }

    @Test
    void rejectsQuantityAboveAvailableStockWithoutWriting() {
        assertThatThrownBy(() -> service.register(12, 7, new StockMovementRequest(StockMovementType.CONSUMED, 6)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(item.getQuantity()).isEqualTo(5);
        verify(items, never()).save(any());
        verifyNoInteractions(movements);
    }

    @Test
    void rejectsMovementOfAnEmptyItem() {
        item.setQuantity(0);
        assertThatThrownBy(() -> service.register(12, 7, new StockMovementRequest(StockMovementType.DISCARDED, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(movements);
    }

    @Test
    void rejectsItemOwnedByAnotherProfile() {
        assertThatThrownBy(() -> service.register(99, 7, new StockMovementRequest(StockMovementType.CONSUMED, 1)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(item.getQuantity()).isEqualTo(5);
        verify(items, never()).save(any());
        verifyNoInteractions(movements);
    }

    @Test
    void rejectsMissingItem() {
        assertThatThrownBy(() -> service.register(12, 8, new StockMovementRequest(StockMovementType.CONSUMED, 1)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(movements);
    }
}
