package com.example.nexuscore.controller;

import com.example.nexuscore.dto.shoppinglist.ShoppingListItemRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListItemUpdateRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListResponse;
import com.example.nexuscore.service.ShoppingListService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Listas de compras")
@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService service;
    private final CurrentProfileResolver currentProfile;

    public ShoppingListController(ShoppingListService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @Operation(summary = "Listar minhas listas",
            description = "Lista documentos do MongoDB pertencentes ao perfil autenticado.")
    @GetMapping
    public List<ShoppingListResponse> list() {
        return service.list(currentProfile.profileId().intValue());
    }

    @Operation(summary = "Criar lista de compras",
            description = "Cria uma lista com título e data opcional para o perfil autenticado.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListResponse create(@Valid @RequestBody ShoppingListRequest request) {
        return service.create(currentProfile.profileId().intValue(), request);
    }

    @Operation(summary = "Consultar lista de compras",
            description = "Consulta uma lista pertencente ao perfil autenticado.")
    @GetMapping("/{id}")
    public ShoppingListResponse get(@PathVariable String id) {
        return service.get(currentProfile.profileId().intValue(), id);
    }

    @Operation(summary = "Atualizar lista de compras",
            description = "Atualiza título e data conforme os campos recebidos.")
    @PutMapping("/{id}")
    public ShoppingListResponse rename(@PathVariable String id, @Valid @RequestBody ShoppingListRequest request) {
        return service.rename(currentProfile.profileId().intValue(), id, request);
    }

    @Operation(summary = "Excluir lista de compras",
            description = "Exclui a lista pertencente ao perfil autenticado.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        service.remove(currentProfile.profileId().intValue(), id);
    }

    @Operation(summary = "Adicionar item à lista",
            description = "Adiciona um item à lista pertencente ao perfil autenticado.")
    @PostMapping("/{id}/items")
    public ShoppingListResponse addItem(@PathVariable String id, @Valid @RequestBody ShoppingListItemRequest request) {
        return service.addItem(currentProfile.profileId().intValue(), id, request);
    }

    @Operation(summary = "Atualizar item da lista",
            description = "Altera os campos informados de um item da lista autenticada.")
    @PatchMapping("/{id}/items/{itemId}")
    public ShoppingListResponse updateItem(@PathVariable String id, @PathVariable String itemId,
                                            @Valid @RequestBody ShoppingListItemUpdateRequest request) {
        return service.updateItem(currentProfile.profileId().intValue(), id, itemId, request);
    }

    @Operation(summary = "Remover item da lista",
            description = "Remove um item e retorna a lista atualizada.")
    @DeleteMapping("/{id}/items/{itemId}")
    public ShoppingListResponse removeItem(@PathVariable String id, @PathVariable String itemId) {
        return service.removeItem(currentProfile.profileId().intValue(), id, itemId);
    }
}
