package com.example.nexuscore.dto.notification;

import java.util.List;

public record NotificationCandidatePage<T>(List<T> content, boolean hasNext) {}
