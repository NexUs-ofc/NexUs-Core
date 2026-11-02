package com.example.nexuscore.dto.store;

import com.example.nexuscore.annotations.TelephoneList;
import com.example.nexuscore.dto.profile.AddressRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

public record StoreCreateRequest(
        @Schema(description = "CNPJ com 14 dígitos, sem pontuação", example = "12345678000195")
        @NotBlank @Pattern(regexp = "\\d{14}") String cnpj,
        @Schema(description = "E-mail único do perfil da loja", example = "loja@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Nome da loja", example = "Mercado Central")
        @NotBlank @Size(max = 150) String name,
        @TelephoneList List<String> phones,
        @Valid @NotNull AddressRequest address,
        @Schema(description = "Abertura; informe também o fechamento", example = "08:00")
        @JsonFormat(pattern = "HH:mm") LocalTime openingTime,
        @Schema(description = "Fechamento posterior à abertura", example = "18:00")
        @JsonFormat(pattern = "HH:mm") LocalTime closingTime
) {}
