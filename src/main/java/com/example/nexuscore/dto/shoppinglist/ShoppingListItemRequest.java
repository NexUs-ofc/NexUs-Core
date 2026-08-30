package com.example.nexuscore.dto.shoppinglist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ShoppingListItemRequest(
        Integer foodId,
        @NotBlank String name,
        @NotNull @Positive Double quantity,
        @NotBlank String unitOfMeasure
) {}
