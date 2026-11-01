package com.example.nexuscore.controller;

import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.CompanyRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.service.ProfileImageService;
import com.example.nexuscore.service.ProfileRestrictionsCache;
import com.example.nexuscore.service.ProfileService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileCnpjTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final ProfileRestrictionsCache restrictions = mock(ProfileRestrictionsCache.class);
    private final Profile profile = mock(Profile.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var current = mock(CurrentProfileResolver.class);
        var service = new ProfileService(profiles, mock(AddressRepository.class),
                mock(GeocodingService.class), restrictions, companies);
        when(current.profileId()).thenReturn(12L);
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(profile.getId()).thenReturn(12);
        when(profile.getName()).thenReturn("Perfil autenticado");
        when(profile.getPhones()).thenReturn(Set.of());
        when(profile.getDietaryRestrictions()).thenReturn(List.of());
        when(restrictions.get(eq(12), any())).thenReturn(List.of());
        mvc = MockMvcBuilders.standaloneSetup(new ProfileController(service, current,
                mock(ProfileImageService.class))).build();
    }

    @Test
    void returnsTheCnpjOfTheAuthenticatedCompany() throws Exception {
        var company = mock(Company.class);
        when(profile.getType()).thenReturn(ProfileType.COMPANY);
        when(companies.findByProfileId(12)).thenReturn(Optional.of(company));
        when(company.getCnpj()).thenReturn("12345678000195");

        mvc.perform(get("/api/profile").param("profileId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.cnpj").value("12345678000195"));

        verify(companies).findByProfileId(12);
    }

    @Test
    void preservesTheCnpjInTheProfileUpdateResponse() throws Exception {
        var company = mock(Company.class);
        when(profile.getType()).thenReturn(ProfileType.COMPANY);
        when(companies.findByProfileId(12)).thenReturn(Optional.of(company));
        when(company.getCnpj()).thenReturn("12345678000195");

        mvc.perform(patch("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("12345678000195"));
    }

    @ParameterizedTest
    @EnumSource(value = ProfileType.class, names = {"HOUSEHOLD", "STORE", "ADMIN"})
    void omitsCnpjForProfilesThatAreNotCompanies(ProfileType type) throws Exception {
        when(profile.getType()).thenReturn(type);

        mvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").doesNotHaveJsonPath());

        verifyNoInteractions(companies);
    }

    @Test
    void doesNotInventACnpjWhenTheCompanyRecordIsMissing() throws Exception {
        when(profile.getType()).thenReturn(ProfileType.COMPANY);
        when(companies.findByProfileId(12)).thenReturn(Optional.empty());

        mvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").doesNotHaveJsonPath());
    }
}
