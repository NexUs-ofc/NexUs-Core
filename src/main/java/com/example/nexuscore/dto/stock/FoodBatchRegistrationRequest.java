package com.example.nexuscore.dto.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FoodBatchRegistrationRequest(
        @NotEmpty @Size(max = 100) List<@NotNull @Valid FoodRegistrationRequest> products
) {}
