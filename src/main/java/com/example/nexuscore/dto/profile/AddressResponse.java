package com.example.nexuscore.dto.profile;

public record AddressResponse(
    String street,
    String number,
    String neighborhood,
    String cep,
    String city,
    String state
) { }
