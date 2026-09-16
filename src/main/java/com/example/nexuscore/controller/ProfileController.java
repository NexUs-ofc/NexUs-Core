package com.example.nexuscore.controller;

import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.dto.profile.ProfileResponse;
import com.example.nexuscore.service.ProfileService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;
    private final CurrentProfileResolver currentProfile;

    public ProfileController(ProfileService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public ProfileResponse get() {
        return service.get(currentProfile.profileId().intValue());
    }

    @PatchMapping
    public ProfileResponse update(@Valid @RequestBody ProfileRequest request) {
        return service.update(currentProfile.profileId().intValue(), request);
    }
}