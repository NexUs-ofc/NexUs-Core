package com.example.nexuscore.controller;

import com.example.nexuscore.dto.store.StoreCreateRequest;
import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.service.StoreService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService service;
    private final CurrentProfileResolver currentProfile;

    public StoreController(StoreService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public List<StoreResponse> list() {
        return service.list();
    }

    @GetMapping("/nearby")
    public List<StoreResponse> nearby(@RequestParam(required = false) String street,
                                       @RequestParam(required = false) String number,
                                       @RequestParam(required = false) String neighborhood,
                                       @RequestParam(required = false) String city,
                                       @RequestParam(required = false) String state,
                                       @RequestParam(required = false) Double radiusKm) {
        return service.nearby(currentProfile.profileId().intValue(), street, number, neighborhood, city, state, radiusKm);
    }

    @GetMapping("/{id}")
    public StoreResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse create(@Valid @RequestBody StoreCreateRequest request) {
        return service.create(currentProfile.profileId().intValue(), request);
    }
}
