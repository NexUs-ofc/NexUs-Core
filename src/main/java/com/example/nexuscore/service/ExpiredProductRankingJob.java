package com.example.nexuscore.service;

import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.repository.PantryItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class ExpiredProductRankingJob {

    private static final int BATCH_SIZE = 500;

    private final PantryItemRepository pantryItemRepository;
    private final ProductRankingService rankingService;

    public ExpiredProductRankingJob(PantryItemRepository pantryItemRepository,
                                    ProductRankingService rankingService) {
        this.pantryItemRepository = pantryItemRepository;
        this.rankingService = rankingService;
    }

    @Scheduled(cron = "${app.rankings.expired-scan-cron:0 5 0 * * *}")
    public void recordExpiredProducts() {
        int pageNumber = 0;
        Slice<PantryItem> page;
        do {
            page = pantryItemRepository.findByExpiryDateBefore(LocalDate.now(),
                    PageRequest.of(pageNumber++, BATCH_SIZE,
                            Sort.by(Sort.Order.asc("expiryDate"), Sort.Order.asc("id"))));
            for (PantryItem item : page.getContent()) {
                rankingService.recordExpired(item.getId(), item.getFood().getId(),
                        item.getProfile().getAddress());
            }
        } while (page.hasNext());
    }
}
