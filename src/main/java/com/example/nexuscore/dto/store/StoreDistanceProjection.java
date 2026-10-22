package com.example.nexuscore.dto.store;

import java.math.BigDecimal;
import java.time.LocalTime;

public interface StoreDistanceProjection {
    Integer getId();
    String getName();
    String getCnpj();
    String getStoreImageUrl();
    String getStreet();
    String getNumber();
    String getNeighborhood();
    String getCity();
    String getState();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    Double getDistanceKm();
    LocalTime getOpeningTime();
    LocalTime getClosingTime();
}
