package com.example.nexuscore.controller;

import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.service.StoreService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

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
    public List<StoreResponse> nearby(@RequestParam(required = false) Double latitude,
                                       @RequestParam(required = false) Double longitude,
                                       @RequestParam(required = false) Double radiusKm) {
        return service.nearby(
                currentProfile.profileId().intValue(),
                latitude, longitude, radiusKm);
    }

    @GetMapping("/{id}")
    public StoreResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

}
