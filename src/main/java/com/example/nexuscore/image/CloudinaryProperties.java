package com.example.nexuscore.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret) {

    public boolean configured() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "CloudinaryProperties[credentials=REDACTED]";
    }
}
