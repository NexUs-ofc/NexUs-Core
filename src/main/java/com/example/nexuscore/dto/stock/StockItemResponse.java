package com.example.nexuscore.dto.stock;

import java.time.LocalDate;

public record StockItemResponse(
        Integer id,
        Integer foodId,
        String foodName,
        String productBrand,
        String foodImageUrl,
        String unitOfMeasure,
        Integer quantity,
        LocalDate expiryDate
) {}
