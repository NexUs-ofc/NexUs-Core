package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.AddressRequest;
import com.example.nexuscore.dto.store.StoreCreateRequest;
import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.dto.store.StoreUpdateRequest;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.geo.GeoPoint;
import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.mapper.StoreMapper;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.model.Store;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository repository;
    private final ProfileRepository profileRepository;
    private final AddressRepository addressRepository;
    private final CompanyService companyService;
    private final GeocodingService geocodingService;

    public StoreService(StoreRepository repository, ProfileRepository profileRepository,
                         AddressRepository addressRepository, CompanyService companyService,
                         GeocodingService geocodingService) {
        this.repository = repository;
        this.profileRepository = profileRepository;
        this.addressRepository = addressRepository;
        this.companyService = companyService;
        this.geocodingService = geocodingService;
    }

    public StoreResponse get(Integer id) {
        return StoreMapper.toResponse(repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loja nao encontrada: " + id)));
    }

    public List<StoreResponse> list() {
        return repository.findAll().stream().map(StoreMapper::toResponse).toList();
    }

    public List<StoreResponse> nearby(Integer profileId, String street, String number, String neighborhood,
                                       String city, String state, Double radiusKm) {
        Optional<GeoPoint> point = hasAddress(street)
                ? geocodingService.geocode(street, number, neighborhood, city, state)
                : geocodingService.geocode(resolveProfileAddress(profileId));
        GeoPoint geoPoint = point.orElseThrow(
                () -> new NotFoundException("Nao foi possivel geocodificar o endereco informado"));

        double radius = radiusKm != null ? radiusKm : 10.0;
        if (radius <= 0 || radius > 100) {
            throw new IllegalArgumentException("Raio deve ser maior que zero e menor ou igual a 100 km");
        }
        return repository.findNearby(geoPoint.latitude().doubleValue(), geoPoint.longitude().doubleValue(), radius)
                .stream().map(StoreMapper::toResponse).toList();
    }


    public List<StoreResponse> listCompany(Integer profileId) {
        Company company = companyService.companyOf(profileId);
        return repository.findByCompanyId(company.getId()).stream().map(StoreMapper::toResponse).toList();
    }

    public StoreResponse getCompany(Integer profileId, Integer id) {
        return StoreMapper.toResponse(findOwned(profileId, id));
    }

    @Transactional
    public StoreResponse createCompanyStore(Integer profileId, StoreCreateRequest request) {
        Company company = companyService.companyOf(profileId);
        if (repository.existsByCnpj(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ ja cadastrado");
        }
        if (profileRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email ja cadastrado");
        }
        long storeCount = repository.countByCompanyId(company.getId());
        if (storeCount >= company.getPlan().getStoreLimit()) {
            throw new IllegalArgumentException("Limite de lojas do plano atingido");
        }

        Address address = saveAddress(null, request.address());
        Profile storeProfile = profileRepository.save(new Profile(
                address, request.email(), request.name(), null,
                ProfileType.STORE,
                request.phones() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.phones())));

        Store store = new Store(request.cnpj(), company, storeProfile);
        return StoreMapper.toResponse(repository.save(store));
    }

    @Transactional
    public StoreResponse updateCompanyStore(Integer profileId, Integer id, StoreUpdateRequest request) {
        Store store = findOwned(profileId, id);
        if (request.cnpj() != null) {
            if (repository.existsByCnpjAndIdNot(request.cnpj(), store.getId())) {
                throw new IllegalArgumentException("CNPJ ja cadastrado");
            }
            store.setCnpj(request.cnpj());
        }

        Profile profile = store.getProfile();
        if (request.name() != null) {
            profile.setName(request.name());
        }
        if (request.phones() != null) {
            profile.setPhones(new LinkedHashSet<>(request.phones()));
        }
        if (request.address() != null) {
            profile.setAddress(saveAddress(profile.getAddress(), request.address()));
        }
        return StoreMapper.toResponse(repository.save(store));
    }

    @Transactional
    public void removeCompanyStore(Integer profileId, Integer id) {
        Store store = findOwned(profileId, id);
        Profile profile = store.getProfile();
        Address address = profile.getAddress();
        repository.delete(store);
        profileRepository.delete(profile);
        if (address != null) {
            addressRepository.delete(address);
        }
    }

    private Store findOwned(Integer profileId, Integer id) {
        Company company = companyService.companyOf(profileId);
        return repository.findByIdAndCompanyId(id, company.getId())
                .orElseThrow(() -> new NotFoundException("Loja nao encontrada: " + id));
    }

    private Address saveAddress(Address current, AddressRequest request) {
        GeoPoint point = geocodingService.geocode(
                        request.street(), request.number(), request.neighborhood(), request.city(), request.state())
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel geocodificar o endereco"));
        Address address = current != null ? current : new Address(
                request.neighborhood(), request.street(), request.number(), request.cep(),
                request.city(), request.state(), point.latitude(), point.longitude());
        if (current != null) {
            address.update(
                    request.neighborhood(), request.street(), request.number(), request.cep(),
                    request.city(), request.state(), point.latitude(), point.longitude());
        }
        return addressRepository.save(address);
    }

    private boolean hasAddress(String street) {
        return street != null && !street.isBlank();
    }

    private Address resolveProfileAddress(Integer profileId) {
        Address address = profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Perfil nao encontrado: " + profileId))
                .getAddress();
        if (address == null) {
            throw new IllegalArgumentException("Perfil autenticado nao possui endereco");
        }
        return address;
    }
}
