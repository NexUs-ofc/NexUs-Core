package com.example.nexuscore.service;

import com.example.nexuscore.dto.ranking.ProductRankingEntry;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.RankingEventType;
import com.example.nexuscore.repository.FoodRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductRankingService {

    private static final String PREFIX = "ranking:product:";
    private static final long DAILY_TTL_SECONDS = Duration.ofDays(90).toSeconds();
    private static final Pattern STATE_CODE = Pattern.compile("[A-Za-z]{2}");
    private static final DefaultRedisScript<Long> APPLY_EVENT = new DefaultRedisScript<>("""
            if redis.call('SET', KEYS[1], '1', 'NX') then
                for index = 2, #KEYS do
                    redis.call('ZINCRBY', KEYS[index], ARGV[1], ARGV[2])
                end
                if ARGV[4] == '1' then
                    redis.call('EXPIRE', KEYS[#KEYS], ARGV[3])
                end
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final FoodRepository foodRepository;

    public ProductRankingService(StringRedisTemplate redis, FoodRepository foodRepository) {
        this.redis = redis;
        this.foodRepository = foodRepository;
    }

    public void recordRegistered(Integer pantryItemId, Integer foodId, Address address) {
        recordEvent(RankingEventType.REGISTERED, pantryItemId, foodId, address);
    }

    public void recordExpired(Integer pantryItemId, Integer foodId, Address address) {
        recordEvent(RankingEventType.EXPIRED, pantryItemId, foodId, address);
    }

    public List<ProductRankingEntry> getRanking(RankingEventType eventType, String state, String city,
                                                 LocalDate date, int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e 100");
        }
        String key = rankingKey(eventType, state, city, date);
        Set<ZSetOperations.TypedTuple<String>> scores = redis.opsForZSet()
                .reverseRangeWithScores(key, 0, limit - 1L);
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }

        List<Integer> foodIds = scores.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .map(Integer::valueOf)
                .toList();
        Map<Integer, Food> foodsById = foodRepository.findAllById(foodIds).stream()
                .collect(Collectors.toMap(Food::getId, food -> food));

        List<ProductRankingEntry> ranking = new ArrayList<>();
        int position = 1;
        for (ZSetOperations.TypedTuple<String> score : scores) {
            Integer foodId = Integer.valueOf(score.getValue());
            Food food = foodsById.get(foodId);
            if (food != null) {
                ranking.add(new ProductRankingEntry(position++, foodId, food.getName(), food.getFoodImageUrl(),
                        score.getScore() == null ? 0 : score.getScore().longValue()));
            }
        }
        return List.copyOf(ranking);
    }

    private void recordEvent(RankingEventType eventType, Integer pantryItemId, Integer foodId, Address address) {
        List<String> keys = new ArrayList<>();
        keys.add("ranking:event:" + eventType.getKey() + ":" + pantryItemId);
        String prefix = PREFIX + eventType.getKey() + ":";
        keys.add(prefix + "all");

        String state = address == null ? null : normalizeState(address.getState());
        if (state != null) {
            keys.add(prefix + "state:" + state);
            if (address.getCity() != null && !address.getCity().isBlank()) {
                keys.add(prefix + "city:" + state + ":" + slug(address.getCity()));
            }
            keys.add(prefix + "day:" + LocalDate.now() + ":state:" + state);
        }

        boolean hasDailyRanking = state != null;
        redis.execute(APPLY_EVENT, keys, "1", foodId.toString(),
                Long.toString(DAILY_TTL_SECONDS), hasDailyRanking ? "1" : "0");
    }

    private String rankingKey(RankingEventType eventType, String state, String city, LocalDate date) {
        String prefix = PREFIX + eventType.getKey() + ":";
        if (date != null) {
            if (state == null || city != null) {
                throw new IllegalArgumentException("O ranking diário exige UF e não aceita cidade");
            }
            return prefix + "day:" + date + ":state:" + normalizeState(state);
        }
        if (city != null && !city.isBlank()) {
            if (state == null) {
                throw new IllegalArgumentException("Informe a UF para consultar o ranking por cidade");
            }
            return prefix + "city:" + normalizeState(state) + ":" + slug(city);
        }
        if (state != null && !state.isBlank()) {
            return prefix + "state:" + normalizeState(state);
        }
        return prefix + "all";
    }

    private String normalizeState(String state) {
        String normalized = state.trim().toUpperCase(Locale.ROOT);
        if (!STATE_CODE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("A UF deve conter duas letras");
        }
        return normalized;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Cidade inválida para consulta de ranking");
        }
        return normalized;
    }
}
