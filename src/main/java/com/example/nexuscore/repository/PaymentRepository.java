package com.example.nexuscore.repository;

import com.example.nexuscore.model.Payment;
import com.example.nexuscore.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByCompanyIdOrderByDueDateDesc(Integer companyId);

    List<Payment> findByCompanyIdAndPaymentStatusOrderByDueDateDesc(
            Integer companyId, PaymentStatus paymentStatus);

    Optional<Payment> findByIdAndCompanyId(Integer id, Integer companyId);
}
