package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.StockMovementRequest;
import com.example.nexuscore.dto.stock.StockMovementResponse;
import com.example.nexuscore.exception.GlobalExceptionHandler;
import com.example.nexuscore.model.StockMovementType;
import com.example.nexuscore.service.StockMovementService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockMovementControllerTest {

    private final StockMovementService service = mock(StockMovementService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CurrentProfileResolver profile = mock(CurrentProfileResolver.class);
        when(profile.profileId()).thenReturn(12L);
        mvc = MockMvcBuilders.standaloneSetup(new StockMovementController(service, profile))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @EnumSource(StockMovementType.class)
    void acceptsMovementForAuthenticatedProfile(StockMovementType type) throws Exception {
        var request = new StockMovementRequest(type, 2);
        when(service.register(12, 7, request)).thenReturn(new StockMovementResponse(
                20, 7, type, 2, 0, LocalDateTime.of(2026, 10, 25, 12, 0)));

        mvc.perform(post("/api/stock/7/movements").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"" + type + "\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value(type.name()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.remainingQuantity").value(0));

        verify(service).register(12, 7, request);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"type\":\"CONSUMED\"}", "{\"quantity\":1}",
        "{\"type\":null,\"quantity\":1}", "{\"type\":\"CONSUMED\",\"quantity\":null}",
        "{\"type\":\"CONSUMED\",\"quantity\":0}", "{\"type\":\"DISCARDED\",\"quantity\":-1}",
        "{\"type\":\"OTHER\",\"quantity\":1}"
    })
    void rejectsInvalidMovementsBeforeCallingService(String body) throws Exception {
        mvc.perform(post("/api/stock/7/movements").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
