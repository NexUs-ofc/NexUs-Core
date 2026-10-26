package com.example.nexuscore.dto.shoppinglist;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

public record ShoppingListResponse(
        String id,
        Integer householdId,
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDate date,
        List<ShoppingListItemResponse> arrayList
) {}
