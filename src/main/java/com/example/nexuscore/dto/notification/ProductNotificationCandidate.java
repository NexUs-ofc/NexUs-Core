package com.example.nexuscore.dto.notification;

import java.time.LocalDate;

public record ProductNotificationCandidate(Integer profileId, Integer itemId, String foodName, LocalDate expiryDate) {}
