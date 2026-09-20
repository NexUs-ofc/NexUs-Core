package com.example.nexuscore.dto.store;

import com.example.nexuscore.annotations.TelephoneList;
import com.example.nexuscore.dto.profile.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StoreCreateRequest(
        @NotBlank @Pattern(regexp = "\\d{14}") String cnpj,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String name,
        @TelephoneList List<String> phones,
        @Valid @NotNull AddressRequest address
) {}
