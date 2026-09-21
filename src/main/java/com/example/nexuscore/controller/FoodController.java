package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.FoodRegistrationRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.service.StockService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final StockService service;
    private final CurrentProfileResolver currentProfile;

    public FoodController(StockService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockItemResponse register(@Valid @RequestBody FoodRegistrationRequest request) {
        return service.registerFood(currentProfile.profileId().intValue(), request);
    }
}
