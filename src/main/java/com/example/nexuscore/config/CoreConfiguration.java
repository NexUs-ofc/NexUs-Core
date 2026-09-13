package com.example.nexuscore.config;

import com.example.nexuscore.geo.GeocodingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AuthProperties.class, GeocodingProperties.class})
public class CoreConfiguration {
}
