package com.example.nexuscore.dto.ranking;

public record ProductRankingEntry(
        Integer position,
        Integer foodId,
        String productName,
        long count
) {}
