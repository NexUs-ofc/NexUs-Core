package com.example.nexuscore.dto.stock;

import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record StockItemUpdateRequest(
        @PositiveOrZero Integer quantity,
        LocalDate expiryDate
) {}
