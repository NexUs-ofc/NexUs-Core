package com.example.nexuscore.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record StockItemRequest(
        @NotNull Integer foodId,
        @NotNull @Positive Integer quantity,
        LocalDate expiryDate
) {}
