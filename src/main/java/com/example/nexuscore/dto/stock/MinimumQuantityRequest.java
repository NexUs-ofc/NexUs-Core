package com.example.nexuscore.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MinimumQuantityRequest(
        @NotNull @PositiveOrZero Integer minimumQuantity
) {}
