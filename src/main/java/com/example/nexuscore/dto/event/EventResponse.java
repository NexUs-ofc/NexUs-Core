package com.example.nexuscore.dto.event;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
        String id,
        Integer householdId,
        String title,
        String description,
        LocalDateTime date,
        Integer duration,
        String location,
        Integer peopleCount,
        List<String> recipeIds
) {}
