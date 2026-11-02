package com.example.nexuscore.controller;

import com.example.nexuscore.dto.notification.EventNotificationCandidate;
import com.example.nexuscore.dto.notification.NearbyStoreCandidate;
import com.example.nexuscore.dto.notification.NewStoreCandidate;
import com.example.nexuscore.dto.notification.NotificationCandidatePage;
import com.example.nexuscore.dto.notification.ProductNotificationCandidate;
import com.example.nexuscore.dto.notification.ShoppingListNotificationCandidate;
import com.example.nexuscore.service.NotificationCandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@Tag(name = "Integração de notificações")
@RestController
@RequestMapping("/internal/notifications")
@SecurityRequirement(name = "NotifyTokenAuth")
public class NotificationCandidateController {

    private final NotificationCandidateService service;

    public NotificationCandidateController(NotificationCandidateService service) {
        this.service = service;
    }

    @Operation(summary = "Consultar candidatos por vencimento",
            description = "Aciona fn_get_expiring_products. Exige X-Notify-Token e intervalo de até 31 dias.")
    @GetMapping("/products")
    public NotificationCandidatePage<ProductNotificationCandidate> products(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.products(from, to, page, size);
    }

    @Operation(summary = "Consultar candidatos de eventos",
            description = "Consulta eventos persistidos para a janela inclusiva informada. Exige X-Notify-Token.")
    @GetMapping("/events")
    public NotificationCandidatePage<EventNotificationCandidate> events(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.events(from, to, page, size);
    }

    @Operation(summary = "Consultar candidatos de listas",
            description = "Seleciona listas com data no intervalo inclusivo. Exige X-Notify-Token.")
    @GetMapping("/shopping-lists")
    public NotificationCandidatePage<ShoppingListNotificationCandidate> shoppingLists(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.shoppingLists(from, to, page, size);
    }

    @Operation(summary = "Consultar lojas próximas por perfil",
            description = "Produz sugestões paginadas por proximidade. Exige X-Notify-Token.")
    @GetMapping("/nearby-stores")
    public NotificationCandidatePage<NearbyStoreCandidate> nearbyStores(
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.nearbyStores(radiusKm, page, size);
    }

    @Operation(summary = "Consultar novas lojas",
            description = "Busca sugestões de lojas ativas cadastradas no intervalo. Exige X-Notify-Token.")
    @GetMapping("/new-stores")
    public NotificationCandidatePage<NewStoreCandidate> newStores(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return service.newStores(from, to, radiusKm, page, size);
    }
}
