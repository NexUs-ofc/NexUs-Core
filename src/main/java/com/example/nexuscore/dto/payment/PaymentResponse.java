package com.example.nexuscore.dto.payment;

import com.example.nexuscore.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Integer id,
        LocalDate dueDate,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        PaymentStatus paymentStatus,
        Integer companyId,
        Integer planId,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt
) {}
