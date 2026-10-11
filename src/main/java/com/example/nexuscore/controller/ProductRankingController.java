package com.example.nexuscore.controller;

import com.example.nexuscore.dto.ranking.ProductRankingEntry;
import com.example.nexuscore.model.RankingEventType;
import com.example.nexuscore.service.ProductRankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rankings/products")
public class ProductRankingController {

    private final ProductRankingService rankingService;

    public ProductRankingController(ProductRankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/{eventType}")
    public List<ProductRankingEntry> getRanking(
            @PathVariable String eventType,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "10") int limit) {
        return rankingService.getRanking(RankingEventType.fromKey(eventType), state, city, date, limit);
    }
}
