package com.example.nexuscore.dto.shoppinglist;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ShoppingListRequest(
        @NotBlank String title,
        LocalDate date
) {}
