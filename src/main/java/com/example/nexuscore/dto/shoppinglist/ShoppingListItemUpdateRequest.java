package com.example.nexuscore.dto.shoppinglist;

import jakarta.validation.constraints.Positive;

public record ShoppingListItemUpdateRequest(
        @Positive Double quantity,
        Boolean checked
) {}
