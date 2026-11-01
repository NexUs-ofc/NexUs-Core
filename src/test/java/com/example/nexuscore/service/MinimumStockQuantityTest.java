package com.example.nexuscore.service;

import com.example.nexuscore.categorization.GpcCategoryResolver;
import com.example.nexuscore.dto.stock.MinimumQuantityRequest;
import com.example.nexuscore.dto.stock.PantryProductSettingResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryProductSetting;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.CategoryRepository;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.PantryProductSettingRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MinimumStockQuantityTest {
    private final PantryItemRepository items = mock(PantryItemRepository.class);
    private final PantryProductSettingRepository settings = mock(PantryProductSettingRepository.class);
    private final FoodRepository foods = mock(FoodRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final ActivityRecordService activities = mock(ActivityRecordService.class);
    private final StockService service = new StockService(items, settings, foods,
            mock(CategoryRepository.class), profiles, mock(GpcCategoryResolver.class),
            mock(ProductRankingService.class), activities);
    private final Food food = mock(Food.class);

    @BeforeEach
    void setUp() {
        when(foods.findById(7)).thenReturn(Optional.of(food));
        when(food.getId()).thenReturn(7);
        when(food.getName()).thenReturn("Arroz");
        when(food.getFoodImageUrl()).thenReturn("https://example.com/arroz.jpg");
    }

    @Test
    void zeroDeletesOnlyTheAuthenticatedProfilesSettingAndPreservesResponseContract() {
        PantryProductSetting setting = mock(PantryProductSetting.class);
        when(settings.findByProfileIdAndFoodId(12, 7)).thenReturn(Optional.of(setting));

        PantryProductSettingResponse response = service.setMinimumQuantity(12, 7, new MinimumQuantityRequest(0));

        assertEquals(new PantryProductSettingResponse(7, "Arroz", "https://example.com/arroz.jpg", 0), response);
        verify(settings).findByProfileIdAndFoodId(12, 7);
        verify(settings).delete(setting);
        verify(settings, never()).save(any());
        verify(activities).record(12, "minimum_stock_quantity_changed",
                "Removeu a configuração de estoque mínimo de Arroz");
        verifyNoInteractions(items, profiles);
    }

    @Test
    void zeroWithoutAnExistingSettingDoesNotCreateOrDeleteAnything() {
        when(settings.findByProfileIdAndFoodId(12, 7)).thenReturn(Optional.empty());

        assertEquals(0, service.setMinimumQuantity(12, 7, new MinimumQuantityRequest(0)).minimumQuantity());

        verify(settings, never()).save(any());
        verify(settings, never()).delete(any());
        verifyNoInteractions(items, profiles, activities);
    }

    @Test
    void positiveMinimumStillCreatesASetting() {
        Profile profile = mock(Profile.class);
        when(settings.findByProfileIdAndFoodId(12, 7)).thenReturn(Optional.empty());
        when(profiles.getReferenceById(12)).thenReturn(profile);
        when(settings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(3, service.setMinimumQuantity(12, 7, new MinimumQuantityRequest(3)).minimumQuantity());

        verify(settings, never()).delete(any());
        verify(profiles).getReferenceById(12);
    }

    @Test
    void positiveMinimumStillUpdatesTheExistingSetting() {
        PantryProductSetting setting = new PantryProductSetting(food, mock(Profile.class), 2);
        when(settings.findByProfileIdAndFoodId(12, 7)).thenReturn(Optional.of(setting));
        when(settings.save(setting)).thenReturn(setting);

        assertEquals(5, service.setMinimumQuantity(12, 7, new MinimumQuantityRequest(5)).minimumQuantity());

        assertEquals(5, setting.getMinimumQuantity());
        verify(settings).save(setting);
        verify(settings, never()).delete(any());
        verifyNoInteractions(profiles);
    }

    @Test
    void zeroForAnUnknownFoodStillReturnsNotFound() {
        when(foods.findById(7)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.setMinimumQuantity(12, 7, new MinimumQuantityRequest(0)));

        verifyNoInteractions(settings, items, profiles, activities);
    }
}
