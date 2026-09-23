package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Store;
import com.example.nexuscore.repository.StoreDistanceProjection;

public final class StoreMapper {

    private StoreMapper() {
    }

    public static StoreResponse toResponse(Store store) {
        Address address = store.getProfile().getAddress();
        return new StoreResponse(
                store.getId(), store.getProfile().getName(), store.getCnpj(),
                address == null ? null : address.getStreet(),
                address == null ? null : address.getNumber(),
                address == null ? null : address.getNeighborhood(),
                address == null ? null : address.getCity(),
                address == null ? null : address.getState(),
                address == null ? null : address.getLatitude(),
                address == null ? null : address.getLongitude(),
                null);
    }

    public static StoreResponse toResponse(StoreDistanceProjection projection) {
        return new StoreResponse(
                projection.getId(), projection.getName(), projection.getCnpj(),
                projection.getStreet(), projection.getNumber(), projection.getNeighborhood(),
                projection.getCity(), projection.getState(), projection.getLatitude(), projection.getLongitude(),
                projection.getDistanceKm());
    }
}
