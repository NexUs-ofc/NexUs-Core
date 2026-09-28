package com.example.nexuscore.service;

import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreServiceTest {

    private final StoreRepository repository = mock(StoreRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final AddressRepository addressRepository = mock(AddressRepository.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final GeocodingService geocodingService = mock(GeocodingService.class);
    private final StoreService service = new StoreService(
            repository, profileRepository, addressRepository, companyService, geocodingService);

    @Test
    void usesCoordinatesReceivedByRequest() {
        when(repository.findNearby(-23.55052, -46.633308, 5.0)).thenReturn(List.of());

        assertEquals(List.of(), service.nearby(12, -23.55052, -46.633308, 5.0));

        verify(repository).findNearby(-23.55052, -46.633308, 5.0);
        verify(profileRepository, never()).findById(12);
    }

    @Test
    void usesProfileCoordinatesWhenRequestDoesNotContainCoordinates() {
        Address address = new Address("Centro", "Rua A", "10", "01001000", "Sao Paulo", "SP",
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));
        Profile profile = mock(Profile.class);
        when(profile.getAddress()).thenReturn(address);
        when(profileRepository.findById(12)).thenReturn(Optional.of(profile));
        when(repository.findNearby(-23.55052, -46.633308, 10.0)).thenReturn(List.of());

        assertEquals(List.of(), service.nearby(12, null, null, null));

        verify(repository).findNearby(-23.55052, -46.633308, 10.0);
    }

    @Test
    void rejectsIncompleteCoordinates() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.nearby(12, -23.55052, null, null));

        assertEquals("Latitude e longitude devem ser informadas juntas", exception.getMessage());
    }

    @Test
    void rejectsProfileAddressWithoutCoordinates() {
        Address address = new Address("Centro", "Rua A", "10", "01001000", "Sao Paulo", "SP", null, null);
        Profile profile = mock(Profile.class);
        when(profile.getAddress()).thenReturn(address);
        when(profileRepository.findById(12)).thenReturn(Optional.of(profile));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.nearby(12, null, null, null));

        assertEquals("Endereco do perfil nao possui latitude e longitude", exception.getMessage());
    }
}
