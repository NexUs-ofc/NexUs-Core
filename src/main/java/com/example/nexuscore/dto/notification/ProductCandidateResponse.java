package com.example.nexuscore.dto.notification;

import java.time.LocalDate;

public record ProductCandidateResponse(
        Integer profileId,
        Integer itemId,
        String foodName,
        LocalDate expiryDate
) {}
