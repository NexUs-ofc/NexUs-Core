package com.example.nexuscore.ratelimit;

import com.example.nexuscore.security.NotifyTokenFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;

public final class IntegrationRateLimitFilter extends OncePerRequestFilter {

    private final InMemoryRateLimiter limiter;
    private final RateLimitProperties properties;

    public IntegrationRateLimitFilter(InMemoryRateLimiter limiter, RateLimitProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!"OPTIONS".equals(request.getMethod()) && authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> NotifyTokenFilter.AUTHORITY.equals(authority.getAuthority()))) {
            long retryAfter = limiter.consume(new InMemoryRateLimiter.Quota(
                    "integration:notify", properties.integrationPerMinute(), Duration.ofMinutes(1)));
            if (retryAfter > 0) {
                RateLimitResponse.write(response, retryAfter);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
