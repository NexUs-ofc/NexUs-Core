package com.example.nexuscore.service;

import com.example.nexuscore.dto.stock.StockMovementRequest;
import com.example.nexuscore.dto.stock.StockMovementResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.StockMovement;
import com.example.nexuscore.repository.PantryItemRepository;
import com.example.nexuscore.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {

    private final PantryItemRepository items;
    private final StockMovementRepository movements;

    public StockMovementService(PantryItemRepository items, StockMovementRepository movements) {
        this.items = items;
        this.movements = movements;
    }

    @Transactional
    public StockMovementResponse register(Integer profileId, Integer itemId, StockMovementRequest request) {
        if (request.type() == null || request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("Informe o tipo e uma quantidade maior que zero");
        }
        PantryItem item = items.findByIdForUpdate(itemId)
                .orElseThrow(() -> new NotFoundException("Item de estoque nao encontrado: " + itemId));
        if (!item.getProfile().getId().equals(profileId)) {
            throw new ForbiddenException("Item de estoque nao pertence ao perfil autenticado");
        }
        if (request.quantity() > item.getQuantity()) {
            throw new IllegalArgumentException("Quantidade solicitada maior que o saldo disponivel");
        }
        item.setQuantity(item.getQuantity() - request.quantity());
        items.save(item);
        StockMovement movement = movements.save(new StockMovement(item, request.type(), request.quantity()));
        return new StockMovementResponse(movement.getId(), item.getId(), movement.getType(), movement.getQuantity(),
                item.getQuantity(), movement.getEventDate());
    }
}
