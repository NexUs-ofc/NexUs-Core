package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.StockItemUpdateRequest;
import com.example.nexuscore.service.StockService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockControllerTest {

    private final StockService service = mock(StockService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CurrentProfileResolver profile = mock(CurrentProfileResolver.class);
        when(profile.profileId()).thenReturn(12L);
        mvc = MockMvcBuilders.standaloneSetup(new StockController(service, profile)).build();
    }

    @Test
    void acceptsCorrectionToZero() throws Exception {
        mvc.perform(patch("/api/stock/7").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
                .andExpect(status().isOk());

        verify(service).update(12, 7, new StockItemUpdateRequest(0, null));
    }

    @Test
    void rejectsNegativeCorrection() throws Exception {
        mvc.perform(patch("/api/stock/7").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
