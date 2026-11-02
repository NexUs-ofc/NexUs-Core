package com.example.nexuscore.controller;

import com.example.nexuscore.dto.store.StoreResponse;
import com.example.nexuscore.service.StoreService;
import com.example.nexuscore.util.CurrentProfileResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Consulta de lojas")
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService service;
    private final CurrentProfileResolver currentProfile;

    public StoreController(StoreService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @Operation(summary = "Listar lojas ativas",
            description = "Lista lojas com perfis de loja e empresa ativos.")
    @GetMapping
    public List<StoreResponse> list() {
        return service.list();
    }

    @Operation(summary = "Buscar lojas próximas",
            description = "Usa as coordenadas informadas em conjunto ou o endereço do perfil autenticado.")
    @GetMapping("/nearby")
    public List<StoreResponse> nearby(@RequestParam(required = false) Double latitude,
                                       @RequestParam(required = false) Double longitude,
                                       @RequestParam(required = false) Double radiusKm) {
        return service.nearby(
                currentProfile.profileId().intValue(),
                latitude, longitude, radiusKm);
    }

    @Operation(summary = "Consultar loja ativa",
            description = "Retorna uma loja com perfil e empresa ativos pelo identificador.")
    @GetMapping("/{id}")
    public StoreResponse get(@PathVariable Integer id) {
        return service.get(id);
    }

}
