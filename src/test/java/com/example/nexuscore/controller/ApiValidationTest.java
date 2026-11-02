package com.example.nexuscore.controller;

import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.exception.ConflictException;
import com.example.nexuscore.exception.GlobalExceptionHandler;
import com.example.nexuscore.service.CompanyService;
import com.example.nexuscore.service.NotificationCandidateService;
import com.example.nexuscore.service.PaymentService;
import com.example.nexuscore.service.ProfileImageService;
import com.example.nexuscore.service.ProfileService;
import com.example.nexuscore.service.StoreService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiValidationTest {

    private final ProfileService profiles = mock(ProfileService.class);
    private final StoreService stores = mock(StoreService.class);
    private final NotificationCandidateService candidates = mock(NotificationCandidateService.class);
    private MockMvc mvc;

    @BeforeEach
    void configure() {
        CurrentProfileResolver current = mock(CurrentProfileResolver.class);
        when(current.profileId()).thenReturn(12L);
        ProfileImageService images = mock(ProfileImageService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProfileController(profiles, current, images),
                        new CompanyController(mock(CompanyService.class), stores, mock(PaymentService.class),
                                current, images), new NotificationCandidateController(candidates))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n", "\u2003"})
    void rejectsBlankNamesOnBothUpdateEndpoints(String name) throws Exception {
        String body = JsonMapper.builder().build().writeValueAsString(Map.of("name", name));
        for (String path : new String[] {"/api/profile", "/api/companies/me/stores/7"}) {
            mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("name:")));
        }
        verifyNoInteractions(profiles, stores);
    }

    @Test
    void allowsOmittingTheNameOnPartialUpdates() throws Exception {
        mvc.perform(patch("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        verify(profiles).update(12, new ProfileRequest(null, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"name\":{}}", ""})
    void returnsTheErrorContractForMalformedBodies(String body) throws Exception {
        mvc.perform(patch("/api/profile").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("JSON")));
        verifyNoInteractions(profiles);
    }

    @Test
    void identifiesInvalidAndMissingParameters() throws Exception {
        mvc.perform(get("/api/companies/me/stores/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido: id"));
        mvc.perform(get("/internal/notifications/products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro obrigatório ausente: from"));
        verifyNoInteractions(stores, candidates);
    }

    @Test
    void preservesHttpSemanticsForUnsupportedMethodsAndMediaTypes() throws Exception {
        mvc.perform(patch("/internal/notifications/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(patch("/api/profile").contentType(MediaType.APPLICATION_XML).content("<profile/>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void treatsIntegrityViolationsAsConflictsWithoutLeakingDatabaseDetails() throws Exception {
        when(profiles.update(eq(12), any())).thenThrow(new DataIntegrityViolationException("SQL password=secret"));
        mvc.perform(patch("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maria\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("duplicidades")))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void preservesTheSafeInternalErrorForDatabaseFailures() throws Exception {
        when(profiles.get(12)).thenThrow(new DataAccessResourceFailureException("password=secret"));
        mvc.perform(get("/api/profile"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Erro interno ao acessar os dados"))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void treatsPreflightDuplicatesAsConflicts() throws Exception {
        when(stores.updateCompanyStore(eq(12), eq(7), any())).thenThrow(new ConflictException("CNPJ ja cadastrado"));
        mvc.perform(patch("/api/companies/me/stores/7")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cnpj\":\"12345678901234\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CNPJ ja cadastrado"));
    }
}
