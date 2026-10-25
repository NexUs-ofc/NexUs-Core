package com.example.nexuscore.controller;

import com.example.nexuscore.dto.shoppinglist.ShoppingListItemUpdateRequest;
import com.example.nexuscore.service.ShoppingListService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
}
