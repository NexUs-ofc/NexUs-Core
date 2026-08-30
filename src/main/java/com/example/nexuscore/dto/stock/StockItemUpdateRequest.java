package com.example.nexuscore.dto.stock;

import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record StockItemUpdateRequest(
        @Positive Integer quantity,
        LocalDate expiryDate
) {}
