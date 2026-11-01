package com.example.nexuscore.service;

import com.example.nexuscore.dto.company.CompanyResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Plan;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.CompanyRepository;
import com.example.nexuscore.repository.PlanRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CompanyPlanQueryTest {

    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final CompanyService service = new CompanyService(companies, plans, profiles, stores);

    @Test
    void readsThePersistedPlanWithoutChangingTheCompany() {
        Profile profile = mock(Profile.class);
        Company company = mock(Company.class);
        Plan plan = mock(Plan.class);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(profile.getType()).thenReturn(ProfileType.COMPANY);
        when(companies.findByProfileId(12)).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(31);
        when(company.getPlan()).thenReturn(plan);
        when(company.getCnpj()).thenReturn("12345678000195");
        when(plan.getId()).thenReturn(3);
        when(plan.getPlanName()).thenReturn("Exclusive");
        when(plan.getPlanPrice()).thenReturn(new BigDecimal("959.99"));
        when(stores.countByCompanyId(31)).thenReturn(8L);

        CompanyResponse result = service.get(12);

        assertEquals(31, result.id());
        assertEquals("12345678000195", result.cnpj());
        assertEquals(3, result.planId());
        assertEquals("Exclusive", result.planName());
        assertEquals(new BigDecimal("959.99"), result.planPrice());
        assertEquals(8L, result.storeCount());
        verify(companies, never()).save(any());
        verifyNoInteractions(plans);
    }

    @Test
    void rejectsProfilesThatAreNotCompanies() {
        Profile profile = mock(Profile.class);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(profile.getType()).thenReturn(ProfileType.HOUSEHOLD);

        assertThrows(ForbiddenException.class, () -> service.get(12));

        verifyNoInteractions(companies, plans, stores);
    }

    @Test
    void rejectsAProfileWithoutACompany() {
        Profile profile = mock(Profile.class);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(profile.getType()).thenReturn(ProfileType.COMPANY);
        when(companies.findByProfileId(12)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(12));

        verifyNoInteractions(plans, stores);
    }
}
