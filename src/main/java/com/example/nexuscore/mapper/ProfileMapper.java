package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.profile.AddressResponse;
import com.example.nexuscore.dto.profile.ProfileResponse;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Profile;
import java.util.List;

public final class ProfileMapper {

    private ProfileMapper() {
    }

    public static ProfileResponse toResponse(Profile profile, List<String> dietaryRestrictions) {
        Address address = profile.getAddress();
        return new ProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getEmail(),
                profile.getType(),
                profile.getStatus(),
                profile.getProfileImageUrl(),
                profile.getCreatedAt(),
                profile.getPhones().stream().toList(),
                toAddressResponse(address), dietaryRestrictions);
    }

    private static AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(address.getStreet(), address.getNumber(), address.getNeighborhood(),
                address.getCep(), address.getCity(), address.getState());
    }
}

