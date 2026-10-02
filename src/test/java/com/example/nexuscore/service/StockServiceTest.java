package com.example.nexuscore.service;

import com.example.nexuscore.categorization.GpcCategoryResolver;
import com.example.nexuscore.dto.stock.PantryProductSettingResponse;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryProductSetting;
import com.example.nexuscore.repository.CategoryRepository;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.PantryProductSettingRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockServiceTest {

    @Test
    void findsMissingProductsInOneRepositoryCall() {
        PantryItemRepository items = mock(PantryItemRepository.class);
        PantryProductSettingRepository settings = mock(PantryProductSettingRepository.class);
        StockService service = new StockService(items, settings, mock(FoodRepository.class),
                mock(CategoryRepository.class), mock(ProfileRepository.class), mock(GpcCategoryResolver.class));
        Food food = mock(Food.class);
        PantryProductSetting setting = mock(PantryProductSetting.class);
        when(food.getId()).thenReturn(7);
        when(food.getName()).thenReturn("Arroz");
        when(setting.getFood()).thenReturn(food);
        when(setting.getMinimumQuantity()).thenReturn(2);
        when(settings.findMissingByProfileId(12)).thenReturn(List.of(setting));

        assertEquals(List.of(new PantryProductSettingResponse(7, "Arroz", 2)), service.missing(12));
        verify(settings).findMissingByProfileId(12);
        verifyNoInteractions(items);
    }
}
