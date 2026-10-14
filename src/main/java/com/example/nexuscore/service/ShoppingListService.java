package com.example.nexuscore.service;

import com.example.nexuscore.dto.shoppinglist.ShoppingListItemRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListItemUpdateRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.ShoppingListMapper;
import com.example.nexuscore.model.ShoppingList;
import com.example.nexuscore.model.ShoppingListItem;
import com.example.nexuscore.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ShoppingListService {

    private final ShoppingListRepository repository;
    private final ActivityRecordService activityRecordService;

    public ShoppingListService(ShoppingListRepository repository, ActivityRecordService activityRecordService) {
        this.repository = repository;
        this.activityRecordService = activityRecordService;
    }

    public List<ShoppingListResponse> list(Integer householdId) {
        return repository.findByHouseholdId(householdId).stream().map(ShoppingListMapper::toResponse).toList();
    }

    public ShoppingListResponse get(Integer householdId, String id) {
        return ShoppingListMapper.toResponse(findOwned(householdId, id));
    }

    public ShoppingListResponse create(Integer householdId, ShoppingListRequest request) {
        ShoppingList list = new ShoppingList(householdId, request.title(), request.eventId());
        ShoppingList saved = repository.save(list);
        activityRecordService.record(householdId, "shopping_list_created",
                "Criou a lista de compras " + request.title());
        return ShoppingListMapper.toResponse(saved);
    }

    public ShoppingListResponse rename(Integer householdId, String id, ShoppingListRequest request) {
        ShoppingList list = findOwned(householdId, id);
        list.setTitle(request.title());
        ShoppingList saved = repository.save(list);
        activityRecordService.record(householdId, "shopping_list_renamed",
                "Renomeou uma lista de compras para " + request.title());
        return ShoppingListMapper.toResponse(saved);
    }

    public void remove(Integer householdId, String id) {
        ShoppingList list = findOwned(householdId, id);
        repository.delete(list);
        activityRecordService.record(householdId, "shopping_list_removed",
                "Removeu a lista de compras " + list.getTitle());
    }

    public ShoppingListResponse addItem(Integer householdId, String id, ShoppingListItemRequest request) {
        ShoppingList list = findOwned(householdId, id);
        list.getArrayList().add(new ShoppingListItem(
                request.foodId(), request.name(), request.quantity(), request.unitOfMeasure(), false));
        ShoppingList saved = repository.save(list);
        activityRecordService.record(householdId, "shopping_item_added",
                "Adicionou " + request.quantity() + " unidade(s) de " + request.name() + " à lista de compras");
        return ShoppingListMapper.toResponse(saved);
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
        ShoppingList saved = repository.save(list);
        activityRecordService.record(householdId, "shopping_item_updated",
                "Atualizou o item " + item.getName() + " na lista de compras");
        return ShoppingListMapper.toResponse(saved);
    }

    public ShoppingListResponse removeItem(Integer householdId, String id, String itemId) {
        ShoppingList list = findOwned(householdId, id);
        ShoppingListItem item = findItem(list, itemId);
        list.getArrayList().remove(item);
        ShoppingList saved = repository.save(list);
        activityRecordService.record(householdId, "shopping_item_removed",
                "Removeu " + item.getName() + " da lista de compras");
        return ShoppingListMapper.toResponse(saved);
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

}
