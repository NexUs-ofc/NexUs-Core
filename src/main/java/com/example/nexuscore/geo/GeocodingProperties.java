package com.example.nexuscore.geo;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.geocoding")
public record GeocodingProperties(@NotBlank String apiKey) {

    @Override
    public String toString() {
        return "GeocodingProperties[apiKey=***]";
    }
}
