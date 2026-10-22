package com.example.nexuscore.dto.store;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalTime;

public record StoreResponse(
        Integer id,
        String name,
        String cnpj,
        String storeImageUrl,
        String street,
        String number,
        String neighborhood,
        String city,
        String state,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceKm,
        @JsonFormat(pattern = "HH:mm") LocalTime openingTime,
        @JsonFormat(pattern = "HH:mm") LocalTime closingTime
) {}
