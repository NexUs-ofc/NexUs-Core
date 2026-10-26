package com.example.nexuscore.controller;

import com.example.nexuscore.dto.shoppinglist.ShoppingListItemUpdateRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListResponse;
import com.example.nexuscore.service.ShoppingListService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShoppingListControllerTest {

    private static final String ITEM_PATH = "/api/shopping-lists/list-id/items/item-id";

    private ShoppingListService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ShoppingListService.class);
        CurrentProfileResolver currentProfile = mock(CurrentProfileResolver.class);
        when(currentProfile.profileId()).thenReturn(12L);
        mockMvc = MockMvcBuilders.standaloneSetup(new ShoppingListController(service, currentProfile)).build();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -0.5, 0.0})
    void rejectsNonPositiveQuantityBeforeCallingService(double quantity) throws Exception {
        mockMvc.perform(patch(ITEM_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":" + quantity + "}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 2.0})
    void acceptsPositiveQuantity(double quantity) throws Exception {
        mockMvc.perform(patch(ITEM_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk());

        verify(service).updateItem(12, "list-id", "item-id", new ShoppingListItemUpdateRequest(quantity, null));
    }

    @Test
    void allowsUpdatingCheckedWithoutQuantity() throws Exception {
        mockMvc.perform(patch(ITEM_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checked\":true}"))
                .andExpect(status().isOk());

        verify(service).updateItem(12, "list-id", "item-id", new ShoppingListItemUpdateRequest(null, true));
    }

    @Test
    void createsListWithDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 10);
        ShoppingListRequest request = new ShoppingListRequest("Compras", date);
        when(service.create(12, request))
                .thenReturn(new ShoppingListResponse("list-id", 12, "Compras", date, List.of()));

        mockMvc.perform(post("/api/shopping-lists").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Compras\",\"date\":\"2026-12-10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value("2026-12-10"))
                .andExpect(jsonPath("$.eventId").doesNotExist());

        verify(service).create(12, request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"title\":\"Compras\"}", "{\"title\":\"Compras\",\"date\":null}"})
    void createsListWithoutDate(String body) throws Exception {
        ShoppingListRequest request = new ShoppingListRequest("Compras", null);
        when(service.create(12, request))
                .thenReturn(new ShoppingListResponse("list-id", 12, "Compras", null, List.of()));

        mockMvc.perform(post("/api/shopping-lists").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").doesNotExist())
                .andExpect(jsonPath("$.eventId").doesNotExist());

        verify(service).create(12, request);
    }

    @Test
    void updatesListWithDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 10);
        ShoppingListRequest request = new ShoppingListRequest("Compras", date);
        when(service.rename(12, "list-id", request))
                .thenReturn(new ShoppingListResponse("list-id", 12, "Compras", date, List.of()));

        mockMvc.perform(put("/api/shopping-lists/list-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Compras\",\"date\":\"2026-12-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-12-10"));

        verify(service).rename(12, "list-id", request);
    }

    @Test
    void returnsDateWhenReadingList() throws Exception {
        when(service.get(12, "list-id")).thenReturn(new ShoppingListResponse("list-id", 12, "Compras",
                LocalDate.of(2026, 12, 10), List.of()));

        mockMvc.perform(get("/api/shopping-lists/list-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-12-10"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2026-02-30", "10/12/2026"})
    void rejectsInvalidDateBeforeCallingService(String date) throws Exception {
        mockMvc.perform(post("/api/shopping-lists").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Compras\",\"date\":\"" + date + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
