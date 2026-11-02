package com.example.nexuscore.ratelimit;

import com.example.nexuscore.config.AuthProperties;
import com.example.nexuscore.config.NotificationIntegrationProperties;
import com.example.nexuscore.config.NotificationIntegrationSecurityConfig;
import com.example.nexuscore.config.SecurityConfig;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RateLimitSecurityTest.TestController.class, properties = {
    "app.rate-limit.requests-per-minute=3",
    "app.rate-limit.writes-per-minute=1",
    "app.rate-limit.integration-per-minute=2",
    "notify.integration.token=notify-token"
})
@Import({SecurityConfig.class, NotificationIntegrationSecurityConfig.class,
        RateLimitSecurityTest.TestSecurityConfiguration.class, RateLimitSecurityTest.TestController.class})
class RateLimitSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class)).build();
    }

    @Test
    void limitsValidatedProfilesOncePerRequestAndKeepsAuthenticationErrors() throws Exception {
        String token = token("101");
        for (int index = 0; index < 3; index++) {
            mvc.perform(get("/api/rate-test").header("Authorization", token)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/rate-test").header("Authorization", token))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        mvc.perform(get("/api/rate-test").header("Authorization", token("102"))).andExpect(status().isOk());
        mvc.perform(get("/api/rate-test").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/rate-test")).andExpect(status().isUnauthorized());
    }

    @Test
    void limitsWritesInAdditionToTheGeneralQuota() throws Exception {
        String token = token("201");
        mvc.perform(post("/api/rate-test").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post("/api/rate-test").header("Authorization", token))
                .andExpect(status().isTooManyRequests());
        mvc.perform(get("/api/rate-test").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/api/rate-test").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/api/rate-test").header("Authorization", token))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void appliesAnIndependentQuotaAfterTheNotifyTokenIsValidated() throws Exception {
        mvc.perform(get("/internal/notifications/products").header("X-Notify-Token", "invalid"))
                .andExpect(status().isUnauthorized());
        for (int index = 0; index < 2; index++) {
            mvc.perform(get("/internal/notifications/products").header("X-Notify-Token", "notify-token"))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/internal/notifications/products").header("X-Notify-Token", "notify-token"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
        mvc.perform(get("/api/rate-test").header("Authorization", token("301"))).andExpect(status().isOk());
    }

    private String token(String profile) {
        var key = new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("nexus-core-test").subject(profile)
                .issuedAt(now).expiresAt(now.plusSeconds(600)).claim("profile_type", "HOUSEHOLD").build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @TestConfiguration
    @EnableWebSecurity
    @EnableConfigurationProperties({AuthProperties.class, NotificationIntegrationProperties.class})
    static class TestSecurityConfiguration {
    }

    @RestController
    static class TestController {
        @GetMapping({"/api/rate-test", "/internal/notifications/products"})
        public String get() {
            return "ok";
        }

        @PostMapping("/api/rate-test")
        public String post() {
            return "ok";
        }
    }
}
