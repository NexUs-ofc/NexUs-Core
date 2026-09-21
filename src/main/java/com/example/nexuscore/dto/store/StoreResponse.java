package com.example.nexuscore.dto.store;

import java.math.BigDecimal;

public record StoreResponse(
        Integer id,
        String name,
        String cnpj,
        String street,
        String number,
        String neighborhood,
        String city,
        String state,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceKm
) {}
