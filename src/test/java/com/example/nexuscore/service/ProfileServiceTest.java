package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.AddressRequest;
import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    @Test
    void keepsCoordinatesWithoutGeocodingWhenOnlyCepChanges() {
        ProfileRepository profiles = mock(ProfileRepository.class);
        AddressRepository addresses = mock(AddressRepository.class);
        GeocodingService geocoding = mock(GeocodingService.class);
        ProfileService service = new ProfileService(profiles, addresses, geocoding);
        Address address = new Address("Centro", "Rua A", "10", "01001000", "Sao Paulo", "SP",
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));
        Profile profile = new Profile(address, "user@example.com", "User", null, ProfileType.HOUSEHOLD, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(addresses.save(address)).thenReturn(address);

        service.update(12, new ProfileRequest(null, null, null,
                new AddressRequest("Centro", "Rua A", "10", "01002000", "Sao Paulo", "SP")));

        assertEquals("01002000", address.getCep());
        assertEquals(new BigDecimal("-23.550520"), address.getLatitude());
        verifyNoInteractions(geocoding);
    }
}
