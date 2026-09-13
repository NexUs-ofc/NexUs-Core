package com.example.nexuscore.geo;

import com.example.nexuscore.model.Address;
import java.util.Optional;

/**
 * Resolve as coordenadas geograficas (latitude/longitude) de um endereco sob demanda,
 * sem depender de coordenadas ja persistidas. Usado tanto para geocodificar o ponto de
 * origem de uma busca de lojas proximas quanto o endereco de uma loja no momento do cadastro.
 */
public interface GeocodingService {

    Optional<GeoPoint> geocode(String street, String number, String neighborhood, String city, String state);

    default Optional<GeoPoint> geocode(Address address) {
        return geocode(address.getStreet(), address.getNumber(), address.getNeighborhood(),
                address.getCity(), address.getState());
    }
}
