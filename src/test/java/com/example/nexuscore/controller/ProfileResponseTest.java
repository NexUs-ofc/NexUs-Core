package com.example.nexuscore.controller;

import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.AddressRepository;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileResponseTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final ProfileRestrictionsCache restrictions = mock(ProfileRestrictionsCache.class);
    private final Profile profile = mock(Profile.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var current = mock(CurrentProfileResolver.class);
        var service = new ProfileService(profiles, mock(AddressRepository.class),
                mock(GeocodingService.class), restrictions);
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

    @ParameterizedTest
    @EnumSource(ProfileType.class)
    void returnsOnlyProfileFieldsForEveryProfileType(ProfileType type) throws Exception {
        when(profile.getType()).thenReturn(type);

        mvc.perform(get("/api/profile").param("profileId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.profileType").value(type.name()))
                .andExpect(jsonPath("$.cnpj").doesNotHaveJsonPath());
    }

    @Test
    void omitsCnpjFromTheCompanyProfileUpdateResponse() throws Exception {
        when(profile.getType()).thenReturn(ProfileType.COMPANY);

        mvc.perform(patch("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").doesNotHaveJsonPath());
    }
}
