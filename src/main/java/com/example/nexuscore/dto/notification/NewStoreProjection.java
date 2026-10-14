package com.example.nexuscore.dto.notification;

import java.time.LocalDateTime;

public interface NewStoreProjection {
    Integer getStoreId();
    String getStoreName();
    LocalDateTime getCreatedAt();
}
