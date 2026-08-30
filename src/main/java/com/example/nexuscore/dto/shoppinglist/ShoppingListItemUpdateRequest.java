package com.example.nexuscore.dto.shoppinglist;

public record ShoppingListItemUpdateRequest(
        Double quantity,
        Boolean checked
) {}
