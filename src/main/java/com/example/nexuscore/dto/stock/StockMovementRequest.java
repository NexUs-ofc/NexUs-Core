package com.example.nexuscore.dto.stock;

import com.example.nexuscore.model.StockMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockMovementRequest(
        @NotNull StockMovementType type,
        @NotNull @Positive Integer quantity
) {}
