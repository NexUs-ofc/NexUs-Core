package com.example.nexuscore.dto.profile;

import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import java.time.LocalDateTime;
import java.util.List;

public record ProfileResponse(
        Integer id,
        String name,
        String email,
        ProfileType profileType,
        ProfileStatus profileStatus,
        String profileImageUrl,
        LocalDateTime createdAt,
        List<String> phones,
        AddressResponse address
) {}
