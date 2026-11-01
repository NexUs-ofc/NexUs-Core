package com.example.nexuscore.dto.notification;

import java.time.LocalDate;

public record ShoppingListNotificationCandidate(
        Integer profileId,
        String shoppingListId,
        String title,
        LocalDate date,
        boolean completed
) {}
