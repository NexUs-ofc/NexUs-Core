package com.example.nexuscore.dto.notification;

public interface NearbyStorePairProjection {
    Integer getProfileId();
    Integer getStoreId();
    String getStoreName();
    double getDistanceKm();
}
