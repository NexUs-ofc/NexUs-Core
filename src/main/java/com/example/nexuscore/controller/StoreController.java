package com.example.nexuscore.controller;

import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.service.StoreService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    @GetMapping
    public List<StoreResponse> list(@RequestParam(required = false) Double lat,
                                     @RequestParam(required = false) Double lng,
                                     @RequestParam(required = false) Double radiusKm) {
        return service.list(lat, lng, radiusKm);
    }

    @GetMapping("/{id}")
    public StoreResponse get(@PathVariable Integer id) {
        return service.get(id);
    }
}
