package com.example.nexuscore.service;

import com.example.nexuscore.dto.shoppinglist.ShoppingListItemRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListItemResponse;
import com.example.nexuscore.dto.shoppinglist.ShoppingListItemUpdateRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.ShoppingList;
import com.example.nexuscore.model.ShoppingListItem;
import com.example.nexuscore.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ShoppingListService {

    private final ShoppingListRepository repository;

    public ShoppingListService(ShoppingListRepository repository) {
        this.repository = repository;
    }

    public List<ShoppingListResponse> list(Integer householdId) {
        return repository.findByHouseholdId(householdId).stream().map(this::toResponse).toList();
    }

    public ShoppingListResponse get(Integer householdId, String id) {
        return toResponse(findOwned(householdId, id));
    }

    public ShoppingListResponse create(Integer householdId, ShoppingListRequest request) {
        ShoppingList list = new ShoppingList(householdId, request.title(), request.eventId());
        return toResponse(repository.save(list));
    }

    public ShoppingListResponse rename(Integer householdId, String id, ShoppingListRequest request) {
        ShoppingList list = findOwned(householdId, id);
        list.setTitle(request.title());
        return toResponse(repository.save(list));
    }

    public void remove(Integer householdId, String id) {
        ShoppingList list = findOwned(householdId, id);
        repository.delete(list);
    }

    public ShoppingListResponse addItem(Integer householdId, String id, ShoppingListItemRequest request) {
        ShoppingList list = findOwned(householdId, id);
        list.getArrayList().add(new ShoppingListItem(
                request.foodId(), request.name(), request.quantity(), request.unitOfMeasure(), false));
        return toResponse(repository.save(list));
    }

    public ShoppingListResponse updateItem(Integer householdId, String id, String itemId,
                                            ShoppingListItemUpdateRequest request) {
        ShoppingList list = findOwned(householdId, id);
        ShoppingListItem item = findItem(list, itemId);
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.checked() != null) {
            item.setChecked(request.checked());
        }
        return toResponse(repository.save(list));
    }

    public ShoppingListResponse removeItem(Integer householdId, String id, String itemId) {
        ShoppingList list = findOwned(householdId, id);
        ShoppingListItem item = findItem(list, itemId);
        list.getArrayList().remove(item);
        return toResponse(repository.save(list));
    }

    private ShoppingListItem findItem(ShoppingList list, String itemId) {
        return list.getArrayList().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Item nao encontrado na lista: " + itemId));
    }

    private ShoppingList findOwned(Integer householdId, String id) {
        ShoppingList list = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lista de compras nao encontrada: " + id));
        if (!list.getHouseholdId().equals(householdId)) {
            throw new ForbiddenException("Lista de compras nao pertence ao household autenticado");
        }
        return list;
    }

    private ShoppingListResponse toResponse(ShoppingList list) {
        return new ShoppingListResponse(
                list.getId(),
                list.getHouseholdId(),
                list.getTitle(),
                list.getEventId(),
                list.getArrayList().stream()
                        .map(item -> new ShoppingListItemResponse(
                                item.getId(), item.getFoodId(), item.getName(),
                                item.getQuantity(), item.getUnitOfMeasure(), item.isChecked()))
                        .toList());
    }
}
