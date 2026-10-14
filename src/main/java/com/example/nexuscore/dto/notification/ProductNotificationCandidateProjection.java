package com.example.nexuscore.dto.notification;

import java.time.LocalDate;

public interface ProductNotificationCandidateProjection {
    Integer getProfileId();
    Integer getItemId();
    String getFoodName();
    LocalDate getExpiryDate();
}
