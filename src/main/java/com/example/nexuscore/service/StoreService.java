package com.example.nexuscore.service;

import com.example.nexuscore.dto.store.StoreCreateRequest;
import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.geo.GeoPoint;
import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.model.Store;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.CompanyRepository;
import com.example.nexuscore.repository.ProfileRepository;
import com.example.nexuscore.repository.StoreDistanceProjection;
import com.example.nexuscore.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository repository;
    private final ProfileRepository profileRepository;
    private final AddressRepository addressRepository;
    private final CompanyRepository companyRepository;
    private final GeocodingService geocodingService;

    public StoreService(StoreRepository repository, ProfileRepository profileRepository,
                         AddressRepository addressRepository, CompanyRepository companyRepository,
                         GeocodingService geocodingService) {
        this.repository = repository;
        this.profileRepository = profileRepository;
        this.addressRepository = addressRepository;
        this.companyRepository = companyRepository;
        this.geocodingService = geocodingService;
    }

    public List<StoreResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Lojas mais proximas de um ponto de origem. O endereco de origem e opcional: se informado,
     * e geocodado sob demanda (Nominatim); caso contrario, usa o endereco ja cadastrado do
     * perfil autenticado. As lojas ja tem lat/lng persistidos desde o cadastro, entao a busca
     * continua sendo uma unica query Haversine, sem geocodificar loja por loja a cada request.
     */
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
                .stream().map(this::toResponse).toList();
    }

    public StoreResponse get(Integer id) {
        Store store = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loja nao encontrada: " + id));
        return toResponse(store);
    }

    /**
     * Cadastra uma loja para a empresa autenticada. O endereco do perfil da loja (ja existente,
     * criado no fluxo de cadastro de conta) e geocodado uma unica vez neste momento e o
     * resultado e persistido - nao e recalculado a cada busca.
     */
    @Transactional
    public StoreResponse create(Integer currentProfileId, StoreCreateRequest request) {
        Company company = companyRepository.findByProfileId(currentProfileId)
                .orElseThrow(() -> new ForbiddenException("Perfil autenticado nao possui uma empresa associada"));
        Profile storeProfile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new NotFoundException("Perfil da loja nao encontrado: " + request.profileId()));
        if (storeProfile.getType() != ProfileType.STORE) {
            throw new IllegalArgumentException("Perfil informado deve ser do tipo STORE");
        }
        if (repository.existsByProfileId(storeProfile.getId())) {
            throw new IllegalArgumentException("Perfil ja esta associado a uma loja");
        }
        if (repository.existsByCnpj(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ ja cadastrado");
        }

        Address address = storeProfile.getAddress();
        if (address == null) {
            throw new IllegalArgumentException("Perfil da loja precisa possuir endereco");
        }
        geocodingService.geocode(address).ifPresent(point -> {
            address.setLatitude(point.latitude());
            address.setLongitude(point.longitude());
            addressRepository.save(address);
        });

        Store store = new Store(request.cnpj(), company, storeProfile);
        return toResponse(repository.save(store));
    }

    private boolean hasAddress(String street) {
        return street != null && !street.isBlank();
    }

    private Address resolveProfileAddress(Integer profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Perfil nao encontrado: " + profileId))
                .getAddress();
    }

    private StoreResponse toResponse(Store store) {
        Address address = store.getProfile().getAddress();
        return new StoreResponse(
                store.getId(), store.getProfile().getName(), store.getCnpj(),
                address.getStreet(), address.getNumber(), address.getNeighborhood(),
                address.getCity(), address.getState(), address.getLatitude(), address.getLongitude(), null);
    }

    private StoreResponse toResponse(StoreDistanceProjection projection) {
        return new StoreResponse(
                projection.getId(), projection.getName(), projection.getCnpj(),
                projection.getStreet(), projection.getNumber(), projection.getNeighborhood(),
                projection.getCity(), projection.getState(), projection.getLatitude(), projection.getLongitude(),
                projection.getDistanceKm());
    }
}
