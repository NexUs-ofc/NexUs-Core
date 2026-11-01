package com.example.nexuscore.dto.stock;

import com.example.nexuscore.model.StockMovementType;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Integer id,
        Integer pantryItemId,
        StockMovementType type,
        Integer quantity,
        Integer remainingQuantity,
        LocalDateTime eventDate
) {}
