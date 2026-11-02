package com.example.nexuscore.controller;

import com.example.nexuscore.dto.stock.FoodResponse;
import com.example.nexuscore.dto.stock.MinimumQuantityRequest;
import com.example.nexuscore.dto.stock.PantryProductSettingResponse;
import com.example.nexuscore.dto.stock.StockItemRequest;
import com.example.nexuscore.dto.stock.StockItemResponse;
import com.example.nexuscore.dto.stock.StockItemUpdateRequest;
import com.example.nexuscore.service.StockService;
import com.example.nexuscore.util.CurrentProfileResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Estoque")
@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService service;
    private final CurrentProfileResolver currentProfile;

    public StockController(StockService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @Operation(summary = "Listar meu estoque",
            description = "Retorna lotes com saldo positivo, ordenados por validade.")
    @GetMapping
    public List<StockItemResponse> list() {
        return service.list(currentProfile.profileId().intValue());
    }

    @Operation(summary = "Adicionar lote ao estoque",
            description = "Adiciona quantidade positiva de um alimento existente ao perfil autenticado.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockItemResponse add(@Valid @RequestBody StockItemRequest request) {
        return service.add(currentProfile.profileId().intValue(), request);
    }

    @Operation(summary = "Atualizar lote de estoque",
            description = "Altera quantidade ou validade de um lote pertencente ao perfil autenticado.")
    @PatchMapping("/{id}")
    public StockItemResponse update(@PathVariable Integer id, @Valid @RequestBody StockItemUpdateRequest request) {
        return service.update(currentProfile.profileId().intValue(), id, request);
    }

    @Operation(summary = "Remover lote do estoque disponível",
            description = "Zera o saldo e preserva o registro para manter o histórico.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Integer id) {
        service.remove(currentProfile.profileId().intValue(), id);
    }

    @Operation(summary = "Consultar produtos abaixo do mínimo",
            description = "Aciona fn_get_items_below_minimum; lotes vencidos não contam para o saldo disponível.")
    @GetMapping("/missing")
    public List<PantryProductSettingResponse> missing() {
        return service.missing(currentProfile.profileId().intValue());
    }

    @Operation(summary = "Listar lotes vencidos",
            description = "Retorna lotes com saldo positivo e validade anterior à data atual.")
    @GetMapping("/expired")
    public List<StockItemResponse> expired() {
        return service.expired(currentProfile.profileId().intValue());
    }

    @Operation(summary = "Pesquisar alimentos",
            description = "Busca no catálogo pelo nome; sem search, retorna todos os alimentos.")
    @GetMapping("/foods")
    public List<FoodResponse> foods(@RequestParam(required = false) String search) {
        return service.searchFoods(search);
    }

    @Operation(summary = "Listar mínimos de estoque",
            description = "Lista as configurações de estoque mínimo do perfil autenticado.")
    @GetMapping("/settings")
    public List<PantryProductSettingResponse> settings() {
        return service.listSettings(currentProfile.profileId().intValue());
    }

    @Operation(summary = "Definir estoque mínimo",
            description = "Define o mínimo de um alimento; informar zero remove sua configuração.")
    @PutMapping("/settings/{foodId}")
    public PantryProductSettingResponse setMinimumQuantity(@PathVariable Integer foodId,
                                                            @Valid @RequestBody MinimumQuantityRequest request) {
        return service.setMinimumQuantity(currentProfile.profileId().intValue(), foodId, request);
    }
}
