package com.example.nexuscore.geo;

import com.example.nexuscore.model.Address;
import java.util.Optional;

public interface GeocodingService {

    Optional<GeoPoint> geocode(String street, String number, String neighborhood, String city, String state);

    default Optional<GeoPoint> geocode(Address address) {
        return geocode(address.getStreet(), address.getNumber(), address.getNeighborhood(),
                address.getCity(), address.getState());
    }
}
