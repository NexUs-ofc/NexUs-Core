package com.example.nexuscore.controller;

import com.example.nexuscore.config.CoreConfiguration;
import com.example.nexuscore.config.NotificationIntegrationSecurityConfig;
import com.example.nexuscore.config.SecurityConfig;
import com.example.nexuscore.dto.notification.NearbyStoreCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreOption;
import com.example.nexuscore.dto.notification.NewStoreCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.security.NotifyTokenFilter;
import com.example.nexuscore.service.NotificationCandidateService;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationCandidateController.class, properties = {
    "notify.integration.token=notify-test-token",
    "app.auth.issuer=nexus-core-test",
    "app.auth.jwt-secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import({NotificationIntegrationSecurityConfig.class, SecurityConfig.class, CoreConfiguration.class,
        NotificationCandidateControllerTest.TestSecurityConfiguration.class})
class NotificationCandidateControllerTest {

    private static final String TOKEN = "notify-test-token";
    private static final LocalDate FROM = LocalDate.of(2026, 10, 12);
    private static final LocalDate TO = LocalDate.of(2026, 10, 15);
    private static final String PRODUCTS = "/internal/notifications/products";
    private static final String NEARBY_STORES = "/internal/notifications/nearby-stores";
    private static final String NEW_STORES = "/internal/notifications/new-stores";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private NotificationCandidateService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class))
                .build();
    }

    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Test
    void returnsProductRecipientsAndPagination() throws Exception {
        when(service.products(FROM, TO, 1, 1)).thenReturn(new NotificationCandidatePage<>(
                List.of(new ProductNotificationCandidate(12, 45, "Leite", TO)), true));

        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString())
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].profileId").value(12))
                .andExpect(jsonPath("$.content[0].itemId").value(45))
                .andExpect(jsonPath("$.content[0].foodName").value("Leite"))
                .andExpect(jsonPath("$.content[0].expiryDate").value("2026-10-15"))
                .andExpect(jsonPath("$.hasNext").value(true));

        verify(service).products(FROM, TO, 1, 1);
    }

    @Test
    void returnsInternalServerErrorForDataAccessFailuresWithAValidNotifyToken() throws Exception {
        when(service.products(FROM, TO, 0, 100)).thenThrow(
                new DataAccessResourceFailureException("Data access failed"));

        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Erro interno ao acessar os dados"));

        verify(service).products(FROM, TO, 0, 100);
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void permitsInternalErrorDispatchWithoutReauthenticating() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setServletPath("/error");
        request.setDispatcherType(DispatcherType.ERROR);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        MockFilterChain chain = new MockFilterChain();

        context.getBean(FilterChainProxy.class).doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(500, response.getStatus());
        verifyNoInteractions(service, jwtDecoder);
    }

    @Test
    void keepsDirectErrorRequestsProtected() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, jwtDecoder);
    }

    @Test
    void exposesNearbyAndNewStoreCandidatesThroughTheNotifyFilter() throws Exception {
        when(service.nearbyStores(5, 0, 100)).thenReturn(new NotificationCandidatePage<>(
                List.of(new NearbyStoreCandidate(12, List.of(new NearbyStoreOption(8, "Loja", 1.5)))), false));
        when(service.newStores(FROM, TO, 5, 0, 100)).thenReturn(new NotificationCandidatePage<>(
                List.of(new NewStoreCandidate(8, "Loja", TO, List.of(12))), false));

        mockMvc.perform(get(NEARBY_STORES).header(NotifyTokenFilter.HEADER_NAME, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].profileId").value(12))
                .andExpect(jsonPath("$.content[0].stores[0].storeId").value(8));
        mockMvc.perform(get(NEW_STORES).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].storeId").value(8))
                .andExpect(jsonPath("$.content[0].profileIds[0]").value(12));

        verify(service).nearbyStores(5, 0, 100);
        verify(service).newStores(FROM, TO, 5, 0, 100);
    }

    @Test
    void rejectsMissingAndInvalidServiceTokens() throws Exception {
        mockMvc.perform(get(PRODUCTS).param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, "wrong-token")
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PRODUCTS).header("Authorization", "Bearer user-token")
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, jwtDecoder);
    }

    @Test
    void rejectsRequestsWhenTheServiceTokenIsNotConfigured() throws Exception {
        for (String token : List.of("", " ")) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", PRODUCTS);
            request.addHeader(NotifyTokenFilter.HEADER_NAME, TOKEN);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            new NotifyTokenFilter(token).doFilter(request, response, chain);

            assertEquals(401, response.getStatus());
            assertNull(chain.getRequest());
        }
    }

    @Test
    void deniesWritesAndKeepsUserEndpointsProtected() throws Exception {
        mockMvc.perform(post(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profile").header(NotifyTokenFilter.HEADER_NAME, TOKEN))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, jwtDecoder);
    }

    @Test
    void rejectsMalformedDatesBeforeCallingTheService() throws Exception {
        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", "invalid").param("to", TO.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsRemovedEventNotificationRoute() throws Exception {
        mockMvc.perform(get("/internal/notifications/events").header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
