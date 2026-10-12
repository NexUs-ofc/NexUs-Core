package com.example.nexuscore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notify.integration")
public record NotificationIntegrationProperties(String token) {}
