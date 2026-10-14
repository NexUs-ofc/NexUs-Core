package com.example.nexuscore.dto.store;

import com.example.nexuscore.annotations.TelephoneList;
import com.example.nexuscore.dto.profile.AddressRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

public record StoreUpdateRequest(
        @Pattern(regexp = "\\d{14}") String cnpj,
        @Size(min = 1, max = 150) String name,
        @TelephoneList List<String> phones,
        @Valid AddressRequest address,
        @JsonFormat(pattern = "HH:mm") LocalTime openingTime,
        @JsonFormat(pattern = "HH:mm") LocalTime closingTime
) {}
