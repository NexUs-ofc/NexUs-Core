package com.example.nexuscore.dto.shoppinglist;

public record ShoppingListItemResponse(
        String id,
        Integer foodId,
        String name,
        Double quantity,
        String unitOfMeasure,
        boolean checked
) {}
