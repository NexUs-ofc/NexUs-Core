package com.example.nexuscore.dto.profile;

import com.example.nexuscore.annotations.TelephoneList;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileRequest(
        @Size(max = 150) String name,
        @Size(max = 500) String profileImageUrl,
        @TelephoneList List<String> phones,
        @Valid AddressRequest address
) {}
