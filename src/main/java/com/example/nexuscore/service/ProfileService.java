package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.AddressRequest;
import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.dto.profile.ProfileResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.geo.GeoPoint;
import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.mapper.ProfileMapper;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Company;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileType;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.CompanyRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository repository;
    private final AddressRepository addressRepository;
    private final GeocodingService geocodingService;
    private final ProfileRestrictionsCache restrictionsCache;
    private final CompanyRepository companies;

    public ProfileService(ProfileRepository repository, AddressRepository addressRepository,
                          GeocodingService geocodingService, ProfileRestrictionsCache restrictionsCache,
                          CompanyRepository companies) {
        this.repository = repository;
        this.addressRepository = addressRepository;
        this.geocodingService = geocodingService;
        this.restrictionsCache = restrictionsCache;
        this.companies = companies;
    }

    public ProfileResponse get(Integer profileId) {
        Profile profile = findProfile(profileId);
        List<String> restrictions = restrictionsCache.get(profileId, profile::getDietaryRestrictions);
        return ProfileMapper.toResponse(profile, restrictions, companyCnpj(profileId, profile));
    }

    @Transactional
    public ProfileResponse update(Integer profileId, ProfileRequest request) {
        Profile profile = findProfile(profileId);
        if (request.name() != null) {
            profile.setName(request.name());
        }
        if (request.phones() != null) {
            profile.setPhones(new LinkedHashSet<>(request.phones()));
        }
        if (request.address() != null) {
            profile.setAddress(saveAddress(profile, request.address()));
        }
        validateRequiredAddress(profile);

        if (request.dietaryRestrictions() != null) {
            profile.setDietaryRestrictions(normalizeRestrictions(request.dietaryRestrictions()));
            restrictionsCache.invalidateAfterCommit(profileId);
        }

        return ProfileMapper.toResponse(profile, profile.getDietaryRestrictions(), companyCnpj(profileId, profile));
    }

    private String companyCnpj(Integer profileId, Profile profile) {
        if (profile.getType() != ProfileType.COMPANY) {
            return null;
        }
        return companies.findByProfileId(profileId).map(Company::getCnpj).orElse(null);
    }

    private List<String> normalizeRestrictions(List<String> restrictions) {
        List<String> normalized = restrictions.stream().map(String::trim).toList();
        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new IllegalArgumentException("Restrições alimentares duplicadas");
        }
        return normalized;
    }

    private void validateRequiredAddress(Profile profile) {
        boolean addressRequired = profile.getType() == ProfileType.HOUSEHOLD || profile.getType() == ProfileType.STORE;
        if (addressRequired && profile.getAddress() == null) {
            throw new IllegalArgumentException("Endereco e obrigatorio para perfis do tipo HOUSEHOLD ou STORE");
        }
    }

    private Address saveAddress(Profile profile, AddressRequest request) {
        Address current = profile.getAddress();
        BigDecimal latitude = null;
        BigDecimal longitude = null;
        if (profile.getType() != ProfileType.COMPANY) {
            GeoPoint point = resolveCoordinates(current, request);
            latitude = point.latitude();
            longitude = point.longitude();
        }
        Address address = current != null ? current : new Address(
                request.neighborhood(), request.street(), request.number(), request.cep(),
                request.city(), request.state(), latitude, longitude);
        if (current != null) {
            address.update(
                    request.neighborhood(), request.street(), request.number(), request.cep(),
                    request.city(), request.state(), latitude, longitude);
        }
        return addressRepository.save(address);
    }

    private GeoPoint resolveCoordinates(Address current, AddressRequest request) {
        if (current != null && current.hasSameLocation(request.street(), request.number(), request.neighborhood(),
                request.city(), request.state()) && current.getLatitude() != null && current.getLongitude() != null) {
            return new GeoPoint(current.getLatitude(), current.getLongitude());
        }
        return geocodingService.geocode(request.street(), request.number(), request.neighborhood(),
                request.city(), request.state())
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel geocodificar o endereco"));
    }

    private Profile findProfile(Integer profileId) {
        return repository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Perfil nao encontrado: " + profileId));
    }
}
