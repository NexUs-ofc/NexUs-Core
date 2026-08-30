package com.example.nexuscore.dto.shoppinglist;

import jakarta.validation.constraints.NotBlank;

public record ShoppingListRequest(
        @NotBlank String title,
        String eventId
) {}
