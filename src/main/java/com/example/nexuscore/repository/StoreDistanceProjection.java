package com.example.nexuscore.repository;

import java.math.BigDecimal;

public interface StoreDistanceProjection {
    Integer getId();
    String getName();
    String getCnpj();
    String getStreet();
    String getNumber();
    String getNeighborhood();
    String getCity();
    String getState();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    Double getDistanceKm();
}
