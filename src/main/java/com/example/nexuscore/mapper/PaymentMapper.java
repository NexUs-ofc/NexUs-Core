package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.payment.PaymentResponse;
import com.example.nexuscore.model.Payment;

public final class PaymentMapper {
    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getDueDate(), payment.getBillingPeriodStart(),
                payment.getBillingPeriodEnd(), payment.getPaymentStatus(),
                payment.getCompany().getId(), payment.getPlan().getId(), payment.getAmount(),
                payment.getCreatedAt(), payment.getUpdatedAt(), payment.getPaidAt());
    }
}
