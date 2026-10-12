package com.example.nexuscore.service;

import com.example.nexuscore.dto.notification.CandidatePageResponse;
import com.example.nexuscore.dto.notification.EventCandidateResponse;
import com.example.nexuscore.dto.notification.ProductCandidateResponse;
import com.example.nexuscore.mapper.EventMapper;
import com.example.nexuscore.mapper.StockMapper;
import com.example.nexuscore.repository.EventRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class NotificationCandidateService {

    private final PantryItemRepository pantryItemRepository;
    private final EventRepository eventRepository;

    public NotificationCandidateService(PantryItemRepository pantryItemRepository, EventRepository eventRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public CandidatePageResponse<ProductCandidateResponse> products(LocalDate from, LocalDate to, int page, int size) {
        validate(from, to, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("expiryDate", "id"));
        Slice<ProductCandidateResponse> candidates = pantryItemRepository
                .findByExpiryDateBetweenAndQuantityGreaterThan(from, to, 0, pageable)
                .map(StockMapper::toNotificationCandidate);
        return new CandidatePageResponse<>(candidates.getContent(), candidates.hasNext());
    }

    public CandidatePageResponse<EventCandidateResponse> events(LocalDate from, LocalDate to, int page, int size) {
        validate(from, to, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("date", "id"));
        Slice<EventCandidateResponse> candidates = eventRepository
                .findByDateGreaterThanEqualAndDateLessThan(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), pageable)
                .map(EventMapper::toNotificationCandidate);
        return new CandidatePageResponse<>(candidates.getContent(), candidates.hasNext());
    }

    private void validate(LocalDate from, LocalDate to, int page, int size) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("As datas inicial e final devem ser informadas");
        }
        if (from.isAfter(to) || to.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("Intervalo de datas invalido");
        }
        if (page < 0) {
            throw new IllegalArgumentException("A pagina deve ser maior ou igual a zero");
        }
        if (size < 1 || size > 500) {
            throw new IllegalArgumentException("O tamanho do lote deve estar entre 1 e 500");
        }
    }
}
