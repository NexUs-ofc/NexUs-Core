package com.example.nexuscore.config;

import com.example.nexuscore.geo.NominatimGeocodingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "CORS_ALLOWED_ORIGINS=https://frontend.example.com, http://localhost:3000")
@AutoConfigureMockMvc
@MockitoBean(types = NominatimGeocodingService.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsSwaggerWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.info.title").value("NexUs Core API"));
    }

    @Test
    void keepsApplicationEndpointsProtected() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsCorsCredentialsForConfiguredOrigins() {
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(
                " https://frontend.example.com, http://localhost:3000, , ");
        HttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/stores/nearby");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly(
                "https://frontend.example.com",
                "http://localhost:3000");
        assertThat(configuration.getAllowedMethods()).isEqualTo(List.of("*"));
        assertThat(configuration.getAllowedHeaders()).isEqualTo(List.of("*"));
        assertThat(configuration.getExposedHeaders()).isEqualTo(List.of("*"));
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void allowsConfiguredLocalhostCorsPreflightWithCredentialsWithoutAuthentication() throws Exception {
        mockMvc.perform(options("/api/stores/nearby")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void allowsConfiguredOriginToAccessSwaggerDocs() throws Exception {
        mockMvc.perform(options("/v3/api-docs")
                        .header("Origin", "https://frontend.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.example.com"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void rejectsCorsPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/stores/nearby")
                        .header("Origin", "https://unknown.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsPreviouslyHardcodedOriginWhenNotConfigured() throws Exception {
        mockMvc.perform(options("/api/stores/nearby")
                        .header("Origin", "https://ceris-core.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsNoOriginsWhenEnvironmentVariableIsEmpty() {
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource("");
        HttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/stores/nearby");

        assertThat(source.getCorsConfiguration(request).getAllowedOrigins()).isEmpty();
    }
}
