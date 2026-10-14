package com.example.nexuscore.model;

public enum RankingEventType {
    REGISTERED("registered"), EXPIRED("expired");

    private final String key;

    RankingEventType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static RankingEventType fromKey(String key) {
        for (RankingEventType eventType : values()) {
            if (eventType.key.equalsIgnoreCase(key)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("Tipo de ranking inválido: " + key);
    }
}
