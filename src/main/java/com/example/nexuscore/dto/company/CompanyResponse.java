package com.example.nexuscore.dto.company;

import java.math.BigDecimal;

public record CompanyResponse(
        Integer id,
        String cnpj,
        Integer planId,
        String planName,
        BigDecimal planPrice,
        Integer storeLimit,
        Long storeCount
) {}
