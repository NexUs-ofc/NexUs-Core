package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.FoodRegistrationRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.exception.GlobalExceptionHandler;
import com.example.nexuscore.model.UnitOfMeasure;
import com.example.nexuscore.service.StockService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FoodControllerTest {

    private static final String PRODUCT = """
            {"gtin":"7891234567890","name":"Arroz","productBrand":"Marca",
             "packageQuantity":1,"unitOfMeasure":"kg","gpcCode":"10000001",
             "quantity":2,"expiryDate":"2026-12-10"}
            """;

    private StockService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(StockService.class);
        CurrentProfileResolver currentProfile = mock(CurrentProfileResolver.class);
        when(currentProfile.profileId()).thenReturn(12L);
        mockMvc = MockMvcBuilders.standaloneSetup(new FoodController(service, currentProfile))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void preservesIndividualRegistration() throws Exception {
        FoodRegistrationRequest request = request("7891234567890");
        when(service.registerFood(12, request)).thenReturn(response(1, 7));

        mockMvc.perform(post("/api/foods").contentType(MediaType.APPLICATION_JSON).content(PRODUCT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.foodId").value(7))
                .andExpect(jsonPath("$.quantity").value(2));

        verify(service).registerFood(12, request);
    }

    @Test
    void registersBatchForAuthenticatedProfileAndReturnsItemsInOrder() throws Exception {
        List<FoodRegistrationRequest> requests = List.of(request("7891234567890"), request("7891234567891"));
        when(service.registerFoods(12, requests)).thenReturn(List.of(response(1, 7), response(2, 8)));

        mockMvc.perform(post("/api/foods/batch").contentType(MediaType.APPLICATION_JSON)
                        .content(batch(PRODUCT, PRODUCT.replace("7891234567890", "7891234567891"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].foodId").value(7))
                .andExpect(jsonPath("$[1].foodId").value(8));

        verify(service).registerFoods(12, requests);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"products\":null}", "{\"products\":[]}", "{\"products\":[null]}"})
    void rejectsMissingEmptyOrNullProducts(String body) throws Exception {
        mockMvc.perform(post("/api/foods/batch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0", "null"})
    void validatesQuantityOfEveryProduct(String quantity) throws Exception {
        String invalidProduct = PRODUCT.replace("\"quantity\":2", "\"quantity\":" + quantity);

        mockMvc.perform(post("/api/foods/batch").contentType(MediaType.APPLICATION_JSON)
                        .content(batch(PRODUCT, invalidProduct)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("products[1].quantity")));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @MethodSource("invalidProducts")
    void validatesProductDetails(String product) throws Exception {
        mockMvc.perform(post("/api/foods/batch").contentType(MediaType.APPLICATION_JSON).content(batch(product)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejectsBatchesAboveOneHundredProducts() throws Exception {
        String products = String.join(",", Collections.nCopies(101, PRODUCT));

        mockMvc.perform(post("/api/foods/batch").contentType(MediaType.APPLICATION_JSON).content(batch(products)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void acceptsOneHundredProducts() throws Exception {
        List<FoodRegistrationRequest> requests = Collections.nCopies(100, request("7891234567890"));
        when(service.registerFoods(12, requests)).thenReturn(Collections.nCopies(100, response(1, 7)));

        mockMvc.perform(post("/api/foods/batch").contentType(MediaType.APPLICATION_JSON)
                        .content(batch(String.join(",", Collections.nCopies(100, PRODUCT)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(100));

        verify(service).registerFoods(12, requests);
    }

    private static Stream<String> invalidProducts() {
        return Stream.of(
                PRODUCT.replace("7891234567890", "invalid"),
                PRODUCT.replace("\"name\":\"Arroz\"", "\"name\":\"\""),
                PRODUCT.replace("\"packageQuantity\":1", "\"packageQuantity\":0"),
                PRODUCT.replace("\"unitOfMeasure\":\"kg\"", "\"unitOfMeasure\":null"));
    }

    private static String batch(String... products) {
        return "{\"products\":[" + String.join(",", products) + "]}";
    }

    private FoodRegistrationRequest request(String gtin) {
        return new FoodRegistrationRequest(gtin, "Arroz", "Marca", null, BigDecimal.ONE,
                UnitOfMeasure.kg, "10000001", 2, LocalDate.of(2026, 12, 10));
    }

    private StockItemResponse response(int id, int foodId) {
        return new StockItemResponse(id, foodId, "Arroz", "Marca", null, "kg", 2, LocalDate.of(2026, 12, 10));
    }
}
