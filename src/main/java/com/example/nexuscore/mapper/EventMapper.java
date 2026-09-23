package com.example.nexuscore.mapper;

import com.example.nexuscore.dto.event.EventResponse;
import com.example.nexuscore.model.Event;

public final class EventMapper {

    private EventMapper() {
    }

    public static EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId().toHexString(), event.getHouseholdId(), event.getTitle(), event.getDescription(),
                event.getDate(), event.getDuration(), event.getLocation(), event.getPeopleCount(),
                event.getRecipes().stream().map(recipe -> recipe.getRecipeId().toHexString()).toList());
    }
}
