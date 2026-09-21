package com.example.nexuscore.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StoreCreateRequest(
        @NotBlank @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 digitos") String cnpj,
        @NotNull Integer profileId
) {}
