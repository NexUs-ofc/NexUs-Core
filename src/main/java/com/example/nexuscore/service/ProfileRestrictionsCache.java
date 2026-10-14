package com.example.nexuscore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ProfileRestrictionsCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileRestrictionsCache.class);
    private static final DefaultRedisScript<Long> CACHE_IF_UNCHANGED = new DefaultRedisScript<>("""
            if (redis.call('GET', KEYS[2]) or '') == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> INVALIDATE = new DefaultRedisScript<>("""
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2])
            return redis.call('DEL', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate redis;
    private final JsonMapper mapper;
    private final long ttlSeconds;

    public ProfileRestrictionsCache(StringRedisTemplate redis, JsonMapper mapper,
            @Value("${app.profile.dietary-restrictions-cache-ttl-seconds:86400}") long ttlSeconds) {
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("O TTL das restrições deve ser maior que zero");
        }
        this.redis = redis;
        this.mapper = mapper;
        this.ttlSeconds = ttlSeconds;
    }

    public List<String> get(Integer profileId, Supplier<List<String>> loader) {
        String key = key(profileId);
        String revision = "";
        boolean available = false;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                try {
                    return List.of(mapper.readValue(cached, String[].class));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Cache de restrições inválido para o perfil {}", profileId);
                }
            }
            String currentRevision = redis.opsForValue().get(key + ":version");
            revision = currentRevision == null ? "" : currentRevision;
            available = true;
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis indisponível ao consultar restrições do perfil {}", profileId);
        }

        List<String> restrictions = List.copyOf(loader.get());
        if (available) {
            try {
                redis.execute(CACHE_IF_UNCHANGED, List.of(key, key + ":version"), revision,
                        mapper.writeValueAsString(restrictions), Long.toString(ttlSeconds));
            } catch (RuntimeException exception) {
                LOGGER.warn("Não foi possível armazenar restrições do perfil {} no Redis", profileId);
            }
        }
        return restrictions;
    }

    public void invalidateAfterCommit(Integer profileId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidate(profileId);
                }
            });
        } else {
            invalidate(profileId);
        }
    }

    private void invalidate(Integer profileId) {
        String key = key(profileId);
        try {
            redis.execute(INVALIDATE, List.of(key, key + ":version"), UUID.randomUUID().toString(),
                    Long.toString(ttlSeconds));
        } catch (RuntimeException exception) {
            LOGGER.warn("Não foi possível invalidar restrições do perfil {} no Redis", profileId);
        }
    }

    private String key(Integer profileId) {
        return "profile:{" + profileId + "}:dietary_restrictions";
    }
}
