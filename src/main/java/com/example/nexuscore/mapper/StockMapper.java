package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.stock.FoodResponse;
import com.example.nexuscore.dto.stock.PantryProductSettingResponse;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.model.Category;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.PantryProductSetting;

public final class StockMapper {
    private StockMapper() {
    }

    public static StockItemResponse toResponse(PantryItem item) {
        return new StockItemResponse(
                item.getId(),
                item.getFood().getId(),
                item.getFood().getName(),
                item.getFood().getProductBrand(),
                item.getFood().getUnitOfMeasure().name(),
                item.getQuantity(),
                item.getExpiryDate());
    }

    public static FoodResponse toResponse(Food food) {
        Category category = food.getCategory();
        return new FoodResponse(
                food.getId(),
                food.getName(),
                category != null ? category.getCategoryName() : null,
                food.getProductBrand(),
                food.getPackageQuantity(),
                food.getUnitOfMeasure().name());
    }

    public static PantryProductSettingResponse toResponse(PantryProductSetting setting) {
        return new PantryProductSettingResponse(
                setting.getFood().getId(),
                setting.getFood().getName(),
                setting.getMinimumQuantity());
    }
}
