package com.example.nexuscore.service;

import com.example.nexuscore.dto.payment.PaymentResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.PaymentMapper;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Payment;
import com.example.nexuscore.model.PaymentStatus;
import com.example.nexuscore.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository repository;
    private final CompanyService companyService;

    public PaymentService(PaymentRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    public List<PaymentResponse> list(Integer profileId, PaymentStatus status) {
        Integer companyId = companyService.companyOf(profileId).getId();
        List<Payment> payments = status == null
                ? repository.findByCompanyIdOrderByDueDateDesc(companyId)
                : repository.findByCompanyIdAndPaymentStatusOrderByDueDateDesc(companyId, status);
        return payments.stream().map(PaymentMapper::toResponse).toList();
    }

    public PaymentResponse get(Integer profileId, Integer id) {
        return PaymentMapper.toResponse(findOwned(profileId, id));
    }

    @Transactional
    public PaymentResponse pay(Integer profileId, Integer id) {
        Payment payment = findOwned(profileId, id);
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Pagamento ja foi realizado");
        }
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalArgumentException("Pagamento foi cancelado");
        }
        payment.pay(LocalDateTime.now());
        return PaymentMapper.toResponse(repository.save(payment));
    }

    private Payment findOwned(Integer profileId, Integer id) {
        Company company = companyService.companyOf(profileId);
        return repository.findByIdAndCompanyId(id, company.getId())
                .orElseThrow(() -> new NotFoundException("Pagamento nao encontrado: " + id));
    }

}
