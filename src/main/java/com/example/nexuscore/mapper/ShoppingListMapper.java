package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.shoppinglist.ShoppingListItemResponse;
import com.example.nexuscore.dto.shoppinglist.ShoppingListResponse;
import com.example.nexuscore.model.ShoppingList;

public final class ShoppingListMapper {
    private ShoppingListMapper() {
    }

    public static ShoppingListResponse toResponse(ShoppingList list) {
        return new ShoppingListResponse(
                list.getId(),
                list.getHouseholdId(),
                list.getTitle(),
                list.getEventId(),
                list.getArrayList().stream()
                        .map(item -> new ShoppingListItemResponse(
                                item.getId(), item.getFoodId(), item.getName(),
                                item.getQuantity(), item.getUnitOfMeasure(), item.isChecked()))
                        .toList());
    }
}
