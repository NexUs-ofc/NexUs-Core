package com.example.nexuscore.dto.stock;

public record PantryProductSettingResponse(
        Integer foodId,
        String foodName,
        Integer minimumQuantity
) {}
