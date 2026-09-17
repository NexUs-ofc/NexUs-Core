package com.example.nexuscore.service;

import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.dto.profile.ProfileResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.ProfileMapper;
import com.example.nexuscore.model.Profile;
import com.example.nexuscore.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashSet;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
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
            profile.updatePhones(new LinkedHashSet<>(request.phones()));
        }
        return ProfileMapper.toResponse(profile);
    }

    private Profile findProfile(Integer profileId) {
        return repository.findById(profileId)
                .orElseThrow(() -> new NotFoundException("Perfil nao encontrado: " + profileId));
    }


}