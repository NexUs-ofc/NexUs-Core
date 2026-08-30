package com.example.nexuscore.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime date,
        @Positive Integer duration,
        String location,
        @Positive Integer peopleCount
) {}
