package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.FoodResponse;
import com.example.nexuscore.dto.stock.MinimumQuantityRequest;
import com.example.nexuscore.dto.stock.PantryProductSettingResponse;
import com.example.nexuscore.dto.stock.StockItemRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.dto.stock.StockItemUpdateRequest;
import com.example.nexuscore.service.StockService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService service;
    private final CurrentProfileResolver currentProfile;

    public StockController(StockService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public List<StockItemResponse> list() {
        return service.list(currentProfile.profileId().intValue());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockItemResponse add(@Valid @RequestBody StockItemRequest request) {
        return service.add(currentProfile.profileId().intValue(), request);
    }

    @PatchMapping("/{id}")
    public StockItemResponse update(@PathVariable Integer id, @Valid @RequestBody StockItemUpdateRequest request) {
        return service.update(currentProfile.profileId().intValue(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Integer id) {
        service.remove(currentProfile.profileId().intValue(), id);
    }

    @GetMapping("/missing")
    public List<PantryProductSettingResponse> missing() {
        return service.missing(currentProfile.profileId().intValue());
    }

    @GetMapping("/expired")
    public List<StockItemResponse> expired() {
        return service.expired(currentProfile.profileId().intValue());
    }

    @GetMapping("/foods")
    public List<FoodResponse> foods(@RequestParam(required = false) String search) {
        return service.searchFoods(search);
    }

    @GetMapping("/settings")
    public List<PantryProductSettingResponse> settings() {
        return service.listSettings(currentProfile.profileId().intValue());
    }

    @PutMapping("/settings/{foodId}")
    public PantryProductSettingResponse setMinimumQuantity(@PathVariable Integer foodId,
                                                            @Valid @RequestBody MinimumQuantityRequest request) {
        return service.setMinimumQuantity(currentProfile.profileId().intValue(), foodId, request);
    }
}
