package com.example.nexuscore.dto.plan;

import java.math.BigDecimal;

public record PlanResponse(Integer id, String planName, BigDecimal planPrice, String description) {}
