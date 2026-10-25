package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.AddressRequest;
import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.geo.GeoPoint;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final GeocodingService geocoding = mock(GeocodingService.class);
    private final ProfileService service = new ProfileService(profiles, addresses, geocoding,
            mock(ProfileRestrictionsCache.class));

    @Test
    void keepsCoordinatesWithoutGeocodingWhenOnlyCepChanges() {
        Address address = new Address("Centro", "Rua A", "10", "01001000", "Sao Paulo", "SP",
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));
        Profile profile = new Profile(address, "user@example.com", "User", null, ProfileType.HOUSEHOLD, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(addresses.save(address)).thenReturn(address);

        service.update(12, new ProfileRequest(null, null, null,
                new AddressRequest("Centro", "Rua A", "10", "01002000", "Sao Paulo", "SP"), null));

        assertEquals("01002000", address.getCep());
        assertEquals(new BigDecimal("-23.550520"), address.getLatitude());
        verifyNoInteractions(geocoding);
    }

    @Test
    void recalculatesAndPersistsCoordinatesWhenAddressChanges() {
        Address address = currentAddress();
        Profile profile = new Profile(address, "user@example.com", "User", null, ProfileType.HOUSEHOLD, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(addresses.save(address)).thenReturn(address);
        when(geocoding.geocode("Estrada São Francisco", "10", "Jardim Henriqueta", "Taboão da Serra", "SP"))
                .thenReturn(Optional.of(new GeoPoint(new BigDecimal("-23.61"), new BigDecimal("-46.77"))));

        service.update(12, new ProfileRequest(null, null, null, changedAddress(), null));

        assertEquals(new BigDecimal("-23.61"), address.getLatitude());
        assertEquals(new BigDecimal("-46.77"), address.getLongitude());
        assertEquals("Taboão da Serra", address.getCity());
        verify(addresses).save(address);
        verify(geocoding).geocode("Estrada São Francisco", "10", "Jardim Henriqueta", "Taboão da Serra", "SP");
    }

    @Test
    void preservesCurrentAddressWhenGeocodingFails() {
        Address address = currentAddress();
        Profile profile = new Profile(address, "user@example.com", "User", null, ProfileType.HOUSEHOLD, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(12, new ProfileRequest(null, null, null, changedAddress(), null)));

        assertEquals("Rua A", address.getStreet());
        assertEquals(new BigDecimal("-23.550520"), address.getLatitude());
        verifyNoInteractions(addresses);
    }

    private Address currentAddress() {
        return new Address("Centro", "Rua A", "10", "01001000", "Sao Paulo", "SP",
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));
    }

    private AddressRequest changedAddress() {
        return new AddressRequest("Jardim Henriqueta", "Estrada São Francisco", "10", "06753000",
                "Taboão da Serra", "SP");
    }
}
