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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final GeocodingService geocoding = mock(GeocodingService.class);
    private final ProfileService service = new ProfileService(profiles, addresses, geocoding,
            mock(ProfileRestrictionsCache.class));

    @Test
    void updatingProfileDetailsPreservesExistingImageUrl() {
        ProfileRepository profiles = mock(ProfileRepository.class);
        ProfileService service = new ProfileService(profiles, mock(AddressRepository.class),
                mock(GeocodingService.class), mock(ProfileRestrictionsCache.class));
        Profile profile = new Profile(null, "company@example.com", "Company", "https://example.com/current.png",
                ProfileType.COMPANY, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));

        service.update(12, new ProfileRequest("New name", null, null, null));

        assertEquals("New name", profile.getName());
        assertEquals("https://example.com/current.png", profile.getProfileImageUrl());
    }

    @Test
    void keepsCoordinatesWithoutGeocodingWhenOnlyCepChanges() {
        Address address = new Address("Centro", "Rua A", "10", "01001000", "Sao Paulo", "SP",
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));
        Profile profile = new Profile(address, "user@example.com", "User", null, ProfileType.HOUSEHOLD, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(addresses.save(address)).thenReturn(address);

        service.update(12, new ProfileRequest(null, null,
                new AddressRequest("Centro", "Rua A", "10", "01002000", "Sao Paulo", "SP"), null));

        assertEquals("01002000", address.getCep());
        assertEquals(new BigDecimal("-23.550520"), address.getLatitude());
        verifyNoInteractions(geocoding);
    }

    @ParameterizedTest
    @EnumSource(value = ProfileType.class, names = {"HOUSEHOLD", "STORE"})
    void recalculatesAndPersistsCoordinatesWhenAddressChanges(ProfileType type) {
        Address address = currentAddress();
        Profile profile = new Profile(address, "user@example.com", "User", null, type, Set.of());
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

    @Test
    void createsCompanyAddressWithoutGeocodingOrCoordinates() {
        Profile profile = new Profile(null, "company@example.com", "Empresa", null, ProfileType.COMPANY, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(addresses.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(12, new ProfileRequest(null, null, null, changedAddress(), null));

        ArgumentCaptor<Address> saved = ArgumentCaptor.forClass(Address.class);
        verify(addresses).save(saved.capture());
        assertEquals("Estrada São Francisco", saved.getValue().getStreet());
        assertEquals("Taboão da Serra", saved.getValue().getCity());
        assertNull(saved.getValue().getLatitude());
        assertNull(saved.getValue().getLongitude());
        verifyNoInteractions(geocoding);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void clearsCompanyCoordinatesWithoutGeocodingWhenSavingAddress(boolean locationChanged) {
        Address address = currentAddress();
        Profile profile = new Profile(address, "company@example.com", "Empresa", null, ProfileType.COMPANY, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));
        when(addresses.save(address)).thenReturn(address);
        AddressRequest request = locationChanged ? changedAddress()
                : new AddressRequest("Centro", "Rua A", "10", "01001000", "Sao Paulo", "SP");

        service.update(12, new ProfileRequest(null, null, null, request, null));

        verify(addresses).save(address);
        assertEquals(request.street(), address.getStreet());
        assertEquals(request.city(), address.getCity());
        assertNull(address.getLatitude());
        assertNull(address.getLongitude());
        verifyNoInteractions(geocoding);
    }

    @Test
    void keepsCompanyAddressOptionalWhenUpdatingOtherFields() {
        Profile profile = new Profile(null, "company@example.com", "Empresa", null, ProfileType.COMPANY, Set.of());
        when(profiles.findById(12)).thenReturn(Optional.of(profile));

        service.update(12, new ProfileRequest("Nova empresa", null, null, null, null));

        assertEquals("Nova empresa", profile.getName());
        assertNull(profile.getAddress());
        verifyNoInteractions(geocoding, addresses);
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
