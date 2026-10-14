package com.example.nexuscore.dto.notification;

import java.time.LocalDateTime;

public record EventCandidateResponse(
        Integer profileId,
        String eventId,
        String title,
        LocalDateTime date
) {}
