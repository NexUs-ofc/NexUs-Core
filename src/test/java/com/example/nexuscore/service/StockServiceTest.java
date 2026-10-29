package com.example.nexuscore.service;

import com.example.nexuscore.categorization.GpcCategoryResolver;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.CategoryRepository;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StockServiceTest {

    @Test
    void removesItemFromAvailableStockWithoutDeletingItsHistorySource() {
        PantryItemRepository items = mock(PantryItemRepository.class);
        StockService service = new StockService(items, mock(FoodRepository.class),
                mock(CategoryRepository.class), mock(ProfileRepository.class),
                mock(GpcCategoryResolver.class), mock(ProductRankingService.class), mock(ActivityRecordService.class));
        Profile profile = mock(Profile.class);
        Food food = mock(Food.class);
        PantryItem item = new PantryItem(food, profile, 3, null);
        when(profile.getId()).thenReturn(12);
        when(items.findByIdForUpdate(7)).thenReturn(Optional.of(item));

        service.remove(12, 7);

        assertEquals(0, item.getQuantity());
        verify(items).save(item);
        verify(items, never()).delete(any());
    }
}
