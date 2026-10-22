package com.example.nexuscore.service;

import com.example.nexuscore.categorization.GpcCategoryResolver;
import com.example.nexuscore.dto.stock.FoodRegistrationRequest;
import com.example.nexuscore.model.Category;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.UnitOfMeasure;
import com.example.nexuscore.repository.CategoryRepository;
import com.example.nexuscore.repository.FoodRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.PantryProductSettingRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoodImageTest {

    private static final String IMAGE_URL = "https://example.com/arroz.jpg";
    private final FoodRepository foods = mock(FoodRepository.class);
    private final PantryItemRepository items = mock(PantryItemRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final GpcCategoryResolver resolver = mock(GpcCategoryResolver.class);
    private final StockService service = new StockService(items, mock(PantryProductSettingRepository.class),
            foods, categories, profiles, resolver,
            mock(ProductRankingService.class), mock(ActivityRecordService.class));

    @BeforeEach
    void setUp() {
        when(profiles.getReferenceById(12)).thenReturn(mock(Profile.class));
        when(items.save(any(PantryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = IMAGE_URL)
    void registersFoodWithOptionalImage(String imageUrl) {
        when(foods.findByGtin("7891234567890")).thenReturn(Optional.empty());
        when(resolver.isKnownFoodBrick("10000001")).thenReturn(true);
        when(resolver.resolve("10000001")).thenReturn("Cereais");
        when(categories.findByCategoryName("Cereais")).thenReturn(Optional.of(new Category("Cereais")));
        when(foods.save(any(Food.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.registerFood(12, request(imageUrl));

        ArgumentCaptor<Food> savedFood = ArgumentCaptor.forClass(Food.class);
        verify(foods).save(savedFood.capture());
        assertEquals(imageUrl, savedFood.getValue().getFoodImageUrl());
        assertEquals(imageUrl, response.foodImageUrl());
    }

    @Test
    void addsImageToExistingFoodWithoutImage() {
        Food food = food(null);
        when(foods.findByGtin("7891234567890")).thenReturn(Optional.of(food));

        assertEquals(IMAGE_URL, service.registerFood(12, request(IMAGE_URL)).foodImageUrl());
        assertEquals(IMAGE_URL, food.getFoodImageUrl());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "https://example.com/another.jpg")
    void preservesExistingCatalogImageWhenRegisteringStock(String requestedImage) {
        when(foods.findByGtin("7891234567890")).thenReturn(Optional.of(food(IMAGE_URL)));

        assertEquals(IMAGE_URL, service.registerFood(12, request(requestedImage)).foodImageUrl());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = IMAGE_URL)
    void includesOptionalImageInProductSearchAndStockLists(String imageUrl) {
        Food food = food(imageUrl);
        when(foods.findAll()).thenReturn(List.of(food));
        when(foods.findByNameContainingIgnoreCase("Arroz")).thenReturn(List.of(food));
        when(items.findByProfileIdOrderByExpiryDateAscIdDesc(12))
                .thenReturn(List.of(new PantryItem(food, mock(Profile.class), 1, null)));

        assertEquals(imageUrl, service.searchFoods(null).get(0).foodImageUrl());
        assertEquals(imageUrl, service.searchFoods("Arroz").get(0).foodImageUrl());
        assertEquals(imageUrl, service.list(12).get(0).foodImageUrl());
    }

    private Food food(String imageUrl) {
        return new Food("Arroz", new Category("Cereais"), "Marca", BigDecimal.ONE,
                UnitOfMeasure.kg, "7891234567890", imageUrl);
    }

    private FoodRegistrationRequest request(String imageUrl) {
        return new FoodRegistrationRequest("7891234567890", "Arroz", "Marca", imageUrl,
                BigDecimal.ONE, UnitOfMeasure.kg, "10000001", 1, null);
    }
}
