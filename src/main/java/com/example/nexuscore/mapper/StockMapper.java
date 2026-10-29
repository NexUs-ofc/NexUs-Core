package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.notification.ProductCandidateResponse;
import com.example.nexuscore.dto.stock.FoodResponse;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.model.Category;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;

public final class StockMapper {
    private StockMapper() {
    }

    public static StockItemResponse toResponse(PantryItem item) {
        return new StockItemResponse(
                item.getId(),
                item.getFood().getId(),
                item.getFood().getName(),
                item.getFood().getProductBrand(),
                item.getFood().getFoodImageUrl(),
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
                food.getFoodImageUrl(),
                food.getPackageQuantity(),
                food.getUnitOfMeasure().name());
    }

    public static ProductCandidateResponse toNotificationCandidate(PantryItem item) {
        return new ProductCandidateResponse(
                item.getProfile().getId(), item.getId(), item.getFood().getName(), item.getExpiryDate());
    }
}
