package com.example.nexuscore.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Erro de validação, regra de negócio ou persistência tratado pelo Core")
public record ApiError(
        @Schema(description = "Instante do erro em UTC") Instant timestamp,
        @Schema(description = "Código HTTP", example = "400") int status,
        @Schema(description = "Nome do status HTTP", example = "Bad Request") String error,
        @Schema(description = "Orientação para o cliente", example = "name: não deve estar em branco") String message
) {}
