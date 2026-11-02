package com.example.nexuscore.dto.stock;

public interface MissingStockProjection {
    Integer getFoodId();

    String getFoodName();

    String getFoodImageUrl();

    Integer getMinimumQuantity();
}
