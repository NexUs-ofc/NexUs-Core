package com.example.nexuscore.dto.company;

import jakarta.validation.constraints.NotNull;

public record PlanChangeRequest(@NotNull Integer planId) {}
