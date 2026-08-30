package com.example.nexuscore.controller;

import com.example.nexuscore.dto.shoppinglist.ShoppingListItemRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListItemUpdateRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListRequest;
import com.example.nexuscore.dto.shoppinglist.ShoppingListResponse;
import com.example.nexuscore.service.ShoppingListService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService service;
    private final CurrentProfileResolver currentProfile;

    public ShoppingListController(ShoppingListService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public List<ShoppingListResponse> list() {
        return service.list(currentProfile.profileId().intValue());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListResponse create(@Valid @RequestBody ShoppingListRequest request) {
        return service.create(currentProfile.profileId().intValue(), request);
    }

    @GetMapping("/{id}")
    public ShoppingListResponse get(@PathVariable String id) {
        return service.get(currentProfile.profileId().intValue(), id);
    }

    @PutMapping("/{id}")
    public ShoppingListResponse rename(@PathVariable String id, @Valid @RequestBody ShoppingListRequest request) {
        return service.rename(currentProfile.profileId().intValue(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        service.remove(currentProfile.profileId().intValue(), id);
    }

    @PostMapping("/{id}/items")
    public ShoppingListResponse addItem(@PathVariable String id, @Valid @RequestBody ShoppingListItemRequest request) {
        return service.addItem(currentProfile.profileId().intValue(), id, request);
    }

    @PatchMapping("/{id}/items/{itemId}")
    public ShoppingListResponse updateItem(@PathVariable String id, @PathVariable String itemId,
                                            @RequestBody ShoppingListItemUpdateRequest request) {
        return service.updateItem(currentProfile.profileId().intValue(), id, itemId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ShoppingListResponse removeItem(@PathVariable String id, @PathVariable String itemId) {
        return service.removeItem(currentProfile.profileId().intValue(), id, itemId);
    }
}
