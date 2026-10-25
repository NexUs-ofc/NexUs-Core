package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.StockMovementRequest;
import com.example.nexuscore.dto.stock.StockMovementResponse;
import com.example.nexuscore.service.StockMovementService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/{itemId}/movements")
public class StockMovementController {

    private final StockMovementService service;
    private final CurrentProfileResolver currentProfile;

    public StockMovementController(StockMovementService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse register(@PathVariable Integer itemId,
                                          @Valid @RequestBody StockMovementRequest request) {
        return service.register(currentProfile.profileId().intValue(), itemId, request);
    }
}
