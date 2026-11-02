package com.example.nexuscore.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(@DefaultValue("100000") int maxEntries,
                                  @DefaultValue("120") int requestsPerMinute,
                                  @DefaultValue("30") int writesPerMinute,
                                  @DefaultValue("600") int integrationPerMinute,
                                  @DefaultValue("") String trustedProxies) {
    public RateLimitProperties {
        if (maxEntries < 1 || requestsPerMinute < 1 || writesPerMinute < 1 || integrationPerMinute < 1) {
            throw new IllegalArgumentException("Rate limits must be positive");
        }
    }
}
