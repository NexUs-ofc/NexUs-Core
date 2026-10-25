package com.example.nexuscore.config;

import com.example.nexuscore.security.NotifyTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class NotificationIntegrationSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain notificationIntegrationFilterChain(HttpSecurity http,
                                                           NotificationIntegrationProperties properties)
            throws Exception {
        return http.securityMatcher("/internal/notifications/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET,
                                "/internal/notifications/products",
                                "/internal/notifications/nearby-stores", "/internal/notifications/new-stores")
                        .hasAuthority(NotifyTokenFilter.AUTHORITY)
                        .anyRequest().denyAll())
                .addFilterBefore(new NotifyTokenFilter(properties.token()), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
