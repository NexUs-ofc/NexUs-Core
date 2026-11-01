package com.example.nexuscore.controller;

import com.example.nexuscore.dto.company.CompanyResponse;
import com.example.nexuscore.dto.company.PlanChangeRequest;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.GlobalExceptionHandler;
import com.example.nexuscore.service.CompanyService;
import com.example.nexuscore.service.PaymentService;
import com.example.nexuscore.service.ProfileImageService;
import com.example.nexuscore.service.StoreService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyPlanControllerTest {

    private final CompanyService companies = mock(CompanyService.class);
    private final CurrentProfileResolver currentProfile = mock(CurrentProfileResolver.class);
    private final CompanyResponse company = new CompanyResponse(31, "12345678000195", 2,
            "Enterprise", new BigDecimal("659.99"), 8L);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        when(currentProfile.profileId()).thenReturn(12L);
        mvc = MockMvcBuilders.standaloneSetup(new CompanyController(companies, mock(StoreService.class),
                        mock(PaymentService.class), currentProfile, mock(ProfileImageService.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void readsTheCompanyOfTheAuthenticatedProfile() throws Exception {
        when(companies.get(12)).thenReturn(company);

        mvc.perform(get("/api/companies/me").param("profileId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.planId").value(2))
                .andExpect(jsonPath("$.planName").value("Enterprise"))
                .andExpect(jsonPath("$.planPrice").value(659.99));

        verify(companies).get(12);
    }

    @Test
    void returnsTheCompanyAfterChangingItsPlan() throws Exception {
        when(companies.changePlan(12, new PlanChangeRequest(2))).thenReturn(company);

        mvc.perform(patch("/api/companies/me/plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(2));

        verify(companies).changePlan(12, new PlanChangeRequest(2));
    }

    @Test
    void rejectsAMissingPlanId() throws Exception {
        mvc.perform(patch("/api/companies/me/plan").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(companies);
    }

    @Test
    void preservesTheCompanyAccessRestriction() throws Exception {
        when(companies.get(12)).thenThrow(new ForbiddenException("Somente empresas podem consultar o plano"));

        mvc.perform(get("/api/companies/me"))
                .andExpect(status().isForbidden());
    }
}
