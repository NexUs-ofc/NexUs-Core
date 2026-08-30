package com.example.nexuscore.dto.shoppinglist;

import java.util.List;

public record ShoppingListResponse(
        String id,
        Integer householdId,
        String title,
        String eventId,
        List<ShoppingListItemResponse> arrayList
) {}
