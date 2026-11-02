package com.example.nexuscore.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record StockItemRequest(
        @Schema(description = "Identificador do alimento existente no catálogo", example = "7")
        @NotNull Integer foodId,
        @Schema(description = "Quantidade positiva do lote", example = "3")
        @NotNull @Positive Integer quantity,
        @Schema(description = "Validade opcional do lote no formato yyyy-MM-dd") LocalDate expiryDate
) {}
