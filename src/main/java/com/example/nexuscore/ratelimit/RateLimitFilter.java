package com.example.nexuscore.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;

public final class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final Set<String> WRITES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final InMemoryRateLimiter limiter;
    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;

    public RateLimitFilter(InMemoryRateLimiter limiter, RateLimitProperties properties,
                           ClientIpResolver clientIpResolver) {
        this.limiter = limiter;
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        return "OPTIONS".equals(request.getMethod()) || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String identity;
        if (authentication instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
            identity = "profile:" + jwt.getToken().getSubject();
        } else {
            String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
            if (!"GET".equals(request.getMethod()) || !"/api/plans".equals(path)) {
                chain.doFilter(request, response);
                return;
            }
            identity = "ip:" + clientIpResolver.resolve(request);
        }
        var general = new InMemoryRateLimiter.Quota("general:" + identity, properties.requestsPerMinute(), MINUTE);
        long retryAfter = WRITES.contains(request.getMethod())
                ? limiter.consume(general, new InMemoryRateLimiter.Quota(
                        "write:" + identity, properties.writesPerMinute(), MINUTE))
                : limiter.consume(general);
        if (retryAfter > 0) {
            RateLimitResponse.write(response, retryAfter);
            return;
        }
        chain.doFilter(request, response);
    }
}
