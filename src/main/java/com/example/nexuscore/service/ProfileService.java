package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.AddressRequest;
import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.dto.profile.ProfileResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.geo.GeoPoint;
import com.example.nexuscore.geo.GeocodingService;
import com.example.nexuscore.mapper.ProfileMapper;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.AddressRepository;
import com.example.nexuscore.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository repository;
    private final AddressRepository addressRepository;
    private final GeocodingService geocodingService;

    public ProfileService(ProfileRepository repository, AddressRepository addressRepository,
                          GeocodingService geocodingService) {
        this.repository = repository;
        this.addressRepository = addressRepository;
        this.geocodingService = geocodingService;
    }

    public ProfileResponse get(Integer profileId) {
        return ProfileMapper.toResponse(findProfile(profileId));
    }

    @Transactional
    public ProfileResponse update(Integer profileId, ProfileRequest  request) {
        Profile profile = findProfile(profileId);
        if (request.name() != null) {
            profile.setName(request.name());
        }
        if (request.profileImageUrl() != null) {
            profile.setProfileImageUrl(request.profileImageUrl());
        }
        if (request.phones() != null) {
            profile.setPhones(new LinkedHashSet<>(request.phones()));
        }
        if (request.address() != null) {
            profile.setAddress(saveAddress(profile.getAddress(), request.address()));
        }
        return ProfileMapper.toResponse(profile);
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

    private Profile findProfile(Integer profileId) {
        return repository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Perfil nao encontrado: " + profileId));
    }
}
