package com.example.nexuscore.geo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geocoding")
public record GeocodingProperties(String userAgent) {}
