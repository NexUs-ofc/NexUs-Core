package com.example.nexuscore.controller;

import com.example.nexuscore.config.CoreConfiguration;
import com.example.nexuscore.config.NotificationIntegrationSecurityConfig;
import com.example.nexuscore.config.SecurityConfig;
import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreOption;
import com.example.nexuscore.dto.notification.NewStoreCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.security.NotifyTokenFilter;
import com.example.nexuscore.service.NotificationCandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final String EVENTS = "/internal/notifications/events";
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
    void returnsEventsUsingTheSameCandidatePageContract() throws Exception {
        LocalDateTime date = TO.atTime(23, 59);
        when(service.events(FROM, TO, 0, 100)).thenReturn(new NotificationCandidatePage<>(
                List.of(new EventNotificationCandidate(19, "event-id", "Jantar", date)), false));

        mockMvc.perform(get(EVENTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].profileId").value(19))
                .andExpect(jsonPath("$.content[0].eventId").value("event-id"))
                .andExpect(jsonPath("$.content[0].title").value("Jantar"))
                .andExpect(jsonPath("$.content[0].date").value("2026-10-15T23:59:00"))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(service).events(FROM, TO, 0, 100);
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
        mockMvc.perform(get(EVENTS).header(NotifyTokenFilter.HEADER_NAME, "wrong-token")
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
        mockMvc.perform(get(EVENTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", "invalid").param("to", TO.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
