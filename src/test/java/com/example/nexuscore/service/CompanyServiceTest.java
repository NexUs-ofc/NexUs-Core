package com.example.nexuscore.service;

import com.example.nexuscore.dto.company.CompanyResponse;
import com.example.nexuscore.dto.company.PlanChangeRequest;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Plan;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.CompanyRepository;
import com.example.nexuscore.repository.PlanRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyServiceTest {

    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final PlanRepository plans = mock(PlanRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final CompanyService service = new CompanyService(companies, plans, profiles, stores);

    @ParameterizedTest
    @ValueSource(longs = {0, 100, 10000})
    void changesPlanRegardlessOfStoreCount(long storeCount) {
        Profile profile = mock(Profile.class);
        Company company = mock(Company.class);
        Plan plan = mock(Plan.class);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(profile.getType()).thenReturn(ProfileType.COMPANY);
        when(companies.findByProfileId(12)).thenReturn(Optional.of(company));
        when(company.getId()).thenReturn(1);
        when(plans.findById(2)).thenReturn(Optional.of(plan));
        when(stores.countByCompanyId(1)).thenReturn(storeCount);
        when(companies.save(company)).thenReturn(company);
        when(company.getPlan()).thenReturn(plan);
        when(plan.getId()).thenReturn(2);

        CompanyResponse response = service.changePlan(12, new PlanChangeRequest(2));

        assertEquals(2, response.planId());
        assertEquals(storeCount, response.storeCount());
        verify(company).setPlan(plan);
        verify(companies).save(company);
    }
}
