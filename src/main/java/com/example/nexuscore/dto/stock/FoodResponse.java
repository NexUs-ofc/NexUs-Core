package com.example.nexuscore.dto.stock;

public record FoodResponse(
        Integer id,
        String name,
        String categoryName,
        String productBrand,
        String foodImageUrl,
        java.math.BigDecimal packageQuantity,
        String unitOfMeasure
) {}
