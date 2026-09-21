package com.example.nexuscore.dto.stock;

import com.example.nexuscore.model.UnitOfMeasure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FoodRegistrationRequest(
        @NotBlank
        @Pattern(regexp = "(?:\\d{8}|\\d{12}|\\d{13}|\\d{14})")
        String gtin,
        @NotBlank String name,
        String productBrand,
        @NotNull @Positive BigDecimal packageQuantity,
        @NotNull UnitOfMeasure unitOfMeasure,
        String gpcCode,
        @NotNull @Positive Integer quantity,
        LocalDate expiryDate
) {}
