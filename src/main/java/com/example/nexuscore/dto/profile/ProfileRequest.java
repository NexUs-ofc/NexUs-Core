package com.example.nexuscore.dto.profile;

import com.example.nexuscore.annotations.TelephoneList;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfileRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String profileImageUrl,
        @TelephoneList List<String> phones
) {}
