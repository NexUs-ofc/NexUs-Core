package com.example.nexuscore.controller;

import com.example.nexuscore.config.CoreConfiguration;
import com.example.nexuscore.config.NotificationIntegrationSecurityConfig;
import com.example.nexuscore.config.SecurityConfig;
import com.example.nexuscore.model.Event;
import com.example.nexuscore.model.Food;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.EventRepository;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.security.NotifyTokenFilter;
import com.example.nexuscore.service.NotificationCandidateService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
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
@Import({NotificationCandidateService.class, NotificationIntegrationSecurityConfig.class,
        SecurityConfig.class, CoreConfiguration.class,
        NotificationCandidateControllerTest.TestSecurityConfiguration.class})
class NotificationCandidateControllerTest {

    private static final String TOKEN = "notify-test-token";
    private static final LocalDate FROM = LocalDate.of(2026, 10, 12);
    private static final LocalDate TO = LocalDate.of(2026, 10, 15);
    private static final String PRODUCTS = "/internal/notifications/products";
    private static final String EVENTS = "/internal/notifications/events";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private PantryItemRepository pantryItemRepository;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class))
                .build();
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Test
    void returnsProductRecipientsAndPagination() throws Exception {
        Food food = mock(Food.class);
        Profile profile = mock(Profile.class);
        PantryItem item = mock(PantryItem.class);
        when(profile.getId()).thenReturn(12);
        when(food.getName()).thenReturn("Leite");
        when(item.getId()).thenReturn(45);
        when(item.getProfile()).thenReturn(profile);
        when(item.getFood()).thenReturn(food);
        when(item.getExpiryDate()).thenReturn(TO);
        PageRequest page = PageRequest.of(1, 1, Sort.by("expiryDate", "id"));
        when(pantryItemRepository.findByExpiryDateBetweenAndQuantityGreaterThan(FROM, TO, 0, page))
                .thenReturn(new SliceImpl<>(List.of(item), page, true));

        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString())
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].profileId").value(12))
                .andExpect(jsonPath("$.content[0].itemId").value(45))
                .andExpect(jsonPath("$.content[0].foodName").value("Leite"))
                .andExpect(jsonPath("$.content[0].expiryDate").value("2026-10-15"))
                .andExpect(jsonPath("$.hasNext").value(true));

        verify(pantryItemRepository).findByExpiryDateBetweenAndQuantityGreaterThan(FROM, TO, 0, page);
        verifyNoInteractions(eventRepository, jwtDecoder);
    }

    @Test
    void includesTheEntireLastDayOfEvents() throws Exception {
        LocalDateTime date = TO.atTime(23, 59);
        Event event = new Event(19, "Jantar", "Familia", date, 60, "Casa", 4);
        ObjectId id = new ObjectId();
        ReflectionTestUtils.setField(event, "id", id);
        PageRequest page = PageRequest.of(0, 100, Sort.by("date", "id"));
        when(eventRepository.findByDateGreaterThanEqualAndDateLessThan(
                FROM.atStartOfDay(), TO.plusDays(1).atStartOfDay(), page))
                .thenReturn(new SliceImpl<>(List.of(event), page, false));

        mockMvc.perform(get(EVENTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].profileId").value(19))
                .andExpect(jsonPath("$.content[0].eventId").value(id.toHexString()))
                .andExpect(jsonPath("$.content[0].title").value("Jantar"))
                .andExpect(jsonPath("$.content[0].date").value("2026-10-15T23:59:00"))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(eventRepository).findByDateGreaterThanEqualAndDateLessThan(
                FROM.atStartOfDay(), TO.plusDays(1).atStartOfDay(), page);
        verifyNoInteractions(pantryItemRepository, jwtDecoder);
    }

    @Test
    void returnsAnEmptyBatchWhenThereAreNoCandidates() throws Exception {
        PageRequest page = PageRequest.of(0, 100, Sort.by("expiryDate", "id"));
        when(pantryItemRepository.findByExpiryDateBetweenAndQuantityGreaterThan(FROM, TO, 0, page))
                .thenReturn(new SliceImpl<>(List.of(), page, false));

        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void rejectsMissingAndInvalidServiceTokens() throws Exception {
        mockMvc.perform(get(PRODUCTS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(EVENTS).header(NotifyTokenFilter.HEADER_NAME, "wrong-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PRODUCTS).header("Authorization", "Bearer user-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(pantryItemRepository, eventRepository, jwtDecoder);
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
        verifyNoInteractions(pantryItemRepository, eventRepository, jwtDecoder);
    }

    @Test
    void rejectsInvalidSearchParametersBeforeQueryingRepositories() throws Exception {
        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", TO.toString()).param("to", FROM.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(EVENTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", FROM.toString()).param("to", TO.toString()).param("page", "-1"))
                .andExpect(status().isBadRequest());
        for (String size : List.of("0", "501")) {
            mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                            .param("from", FROM.toString()).param("to", TO.toString()).param("size", size))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get(PRODUCTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("to", TO.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(EVENTS).header(NotifyTokenFilter.HEADER_NAME, TOKEN)
                        .param("from", "invalid").param("to", TO.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pantryItemRepository, eventRepository);
    }
}
