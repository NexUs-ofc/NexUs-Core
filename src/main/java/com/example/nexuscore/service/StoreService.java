package com.example.nexuscore.service;

import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Address;
import com.example.nexuscore.model.Store;
import com.example.nexuscore.repository.StoreDistanceProjection;
import com.example.nexuscore.repository.StoreRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository repository;

    public StoreService(StoreRepository repository) {
        this.repository = repository;
    }

    public List<StoreResponse> list(Double lat, Double lng, Double radiusKm) {
        if (lat != null && lng != null) {
            double radius = radiusKm != null ? radiusKm : 10.0;
            return repository.findNearby(lat, lng, radius).stream().map(this::toResponse).toList();
        }
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public StoreResponse get(Integer id) {
        Store store = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loja nao encontrada: " + id));
        return toResponse(store);
    }

    private StoreResponse toResponse(Store store) {
        Address address = store.getProfile().getAddress();
        return new StoreResponse(
                store.getId(), store.getProfile().getName(), store.getCnpj(),
                address.getStreet(), address.getNumber(), address.getNeighborhood(),
                address.getCity(), address.getState(), address.getLatitude(), address.getLongitude(), null);
    }

    private StoreResponse toResponse(StoreDistanceProjection projection) {
        return new StoreResponse(
                projection.getId(), projection.getName(), projection.getCnpj(),
                projection.getStreet(), projection.getNumber(), projection.getNeighborhood(),
                projection.getCity(), projection.getState(), projection.getLatitude(), projection.getLongitude(),
                projection.getDistanceKm());
    }
}
