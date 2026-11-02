package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.FoodBatchRegistrationRequest;
import com.example.nexuscore.dto.stock.FoodRegistrationRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.service.StockService;
import com.example.nexuscore.util.CurrentProfileResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Alimentos")
@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final StockService service;
    private final CurrentProfileResolver currentProfile;

    public FoodController(StockService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @Operation(summary = "Cadastrar alimento no estoque",
            description = "Reutiliza o GTIN ou cria alimento com GPC conhecido e adiciona um lote.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockItemResponse register(@Valid @RequestBody FoodRegistrationRequest request) {
        return service.registerFood(currentProfile.profileId().intValue(), request);
    }

    @Operation(summary = "Cadastrar alimentos em lote",
            description = "Cadastra de 1 a 100 produtos em uma única transação.")
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<StockItemResponse> registerBatch(@Valid @RequestBody FoodBatchRegistrationRequest request) {
        return service.registerFoods(currentProfile.profileId().intValue(), request.products());
    }
}
