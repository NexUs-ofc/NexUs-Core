package com.example.nexuscore.controller;

import com.example.nexuscore.dto.profile.ProfileImageResponse;
import com.example.nexuscore.dto.profile.ProfileRequest;
import com.example.nexuscore.dto.profile.ProfileResponse;
import com.example.nexuscore.service.ProfileImageService;
import com.example.nexuscore.service.ProfileService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;
    private final CurrentProfileResolver currentProfile;
    private final ProfileImageService images;

    public ProfileController(ProfileService service, CurrentProfileResolver currentProfile,
                             ProfileImageService images) {
        this.service = service;
        this.currentProfile = currentProfile;
        this.images = images;
    }

    @GetMapping
    public ProfileResponse get() {
        return service.get(currentProfile.profileId().intValue());
    }

    @PatchMapping
    public ProfileResponse update(@Valid @RequestBody ProfileRequest request) {
        return service.update(currentProfile.profileId().intValue(), request);
    }

    @PutMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileImageResponse uploadImage(@RequestParam("file") MultipartFile file) {
        return images.uploadOwn(currentProfile.profileId().intValue(), file);
    }

    @DeleteMapping("/image")
    public ProfileImageResponse removeImage() {
        return images.removeOwn(currentProfile.profileId().intValue());
    }
}
