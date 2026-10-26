package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.AddressRequest;
import com.example.nexuscore.dto.store.StoreCreateRequest;
import com.example.nexuscore.dto.store.StoreDistanceProjection;
import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.dto.store.StoreUpdateRequest;
import com.example.nexuscore.geo.GeoPoint;
import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Plan;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.model.Store;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StoreServiceTest {

    private final StoreRepository repository = mock(StoreRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final AddressRepository addressRepository = mock(AddressRepository.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final GeocodingService geocodingService = mock(GeocodingService.class);
    private final StoreService service = new StoreService(
            repository, profileRepository, addressRepository, companyService, geocodingService);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "https://example.com/store.jpg")
    void includesProfileImageInStoreDetailsAndList(String imageUrl) {
        Profile profile = mock(Profile.class);
        Store store = mock(Store.class);
        when(store.getProfile()).thenReturn(profile);
        when(profile.getProfileImageUrl()).thenReturn(imageUrl);
        when(repository.findByIdForActiveProfiles(1, ProfileStatus.ACTIVE, ProfileStatus.ACTIVE))
                .thenReturn(Optional.of(store));
        when(repository.findAllForActiveProfiles(ProfileStatus.ACTIVE, ProfileStatus.ACTIVE))
                .thenReturn(List.of(store));

        assertEquals(imageUrl, service.get(1).storeImageUrl());
        assertEquals(imageUrl, service.list().get(0).storeImageUrl());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "https://example.com/store.jpg")
    void includesProfileImageInNearbyStores(String imageUrl) {
        StoreDistanceProjection projection = mock(StoreDistanceProjection.class);
        when(projection.getStoreImageUrl()).thenReturn(imageUrl);
        when(repository.findNearby(-23.55052, -46.633308, 5.0)).thenReturn(List.of(projection));

        assertEquals(imageUrl, service.nearby(12, -23.55052, -46.633308, 5.0).get(0).storeImageUrl());
    }

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
        when(repository.findNearby(-23.55052, -46.633308, 5.0)).thenReturn(List.of());

        assertEquals(List.of(), service.nearby(12, null, null, null));

        verify(repository).findNearby(-23.55052, -46.633308, 5.0);
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

    @Test
    void geocodesAndPersistsAddressWhenCreatingStore() {
        configureCompany();
        configureGeocoding();
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = service.createCompanyStore(12, new StoreCreateRequest("12345678000199",
                "store@example.com", "Loja", null, changedAddress(), null, null));

        assertEquals(new BigDecimal("-23.61"), response.latitude());
        assertEquals(new BigDecimal("-46.77"), response.longitude());
        assertEquals("Taboão da Serra", response.city());
        verify(geocodingService).geocode("Estrada São Francisco", "10", "Jardim Henriqueta", "Taboão da Serra", "SP");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void recalculatesCoordinatesWhenUpdatingStoreAddress() {
        Company company = configureCompany();
        configureGeocoding();
        Address address = new Address("Centro", "Rua A", "10", "01001000", "São Paulo", "SP",
                new BigDecimal("-23.550520"), new BigDecimal("-46.633308"));
        Profile profile = new Profile(address, "store@example.com", "Loja", null, ProfileType.STORE, Set.of());
        Store store = new Store("12345678000199", company, profile);
        ReflectionTestUtils.setField(store, "id", 7);
        when(repository.findByIdAndCompanyId(7, 3)).thenReturn(Optional.of(store));
        when(addressRepository.save(address)).thenReturn(address);
        when(repository.save(store)).thenReturn(store);

        service.updateCompanyStore(12, 7, new StoreUpdateRequest(null, null, null, changedAddress(), null, null));

        assertEquals(new BigDecimal("-23.61"), address.getLatitude());
        assertEquals(new BigDecimal("-46.77"), address.getLongitude());
        verify(geocodingService).geocode("Estrada São Francisco", "10", "Jardim Henriqueta", "Taboão da Serra", "SP");
        verify(addressRepository).save(address);
    }

    @Test
    void rejectsStoreCreationWhenGeocodingFails() {
        configureCompany();

        assertThrows(IllegalArgumentException.class, () -> service.createCompanyStore(12,
                new StoreCreateRequest("12345678000199", "store@example.com", "Loja", null,
                        changedAddress(), null, null)));

        verifyNoInteractions(addressRepository);
        verify(profileRepository, never()).save(any(Profile.class));
        verify(repository, never()).save(any(Store.class));
    }

    private Company configureCompany() {
        Company company = mock(Company.class);
        Plan plan = mock(Plan.class);
        when(companyService.companyOf(12)).thenReturn(company);
        when(company.getId()).thenReturn(3);
        when(company.getPlan()).thenReturn(plan);
        when(plan.getStoreLimit()).thenReturn(2);
        return company;
    }

    private void configureGeocoding() {
        when(geocodingService.geocode("Estrada São Francisco", "10", "Jardim Henriqueta", "Taboão da Serra", "SP"))
                .thenReturn(Optional.of(new GeoPoint(new BigDecimal("-23.61"), new BigDecimal("-46.77"))));
    }

    private AddressRequest changedAddress() {
        return new AddressRequest("Jardim Henriqueta", "Estrada São Francisco", "10", "06753000",
                "Taboão da Serra", "SP");
    }
}
