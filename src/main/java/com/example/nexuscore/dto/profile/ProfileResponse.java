package com.example.nexuscore.dto.profile;

import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.ProfileType;
import com.fasterxml.jackson.annotation.JsonInclude;
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
        AddressResponse address,
        List<String> dietaryRestrictions,
        @JsonInclude(JsonInclude.Include.NON_NULL) String cnpj
) {}
