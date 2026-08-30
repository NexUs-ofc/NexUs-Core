package com.example.nexuscore.service;

import com.example.nexuscore.dto.event.EventRequest;
import com.example.nexuscore.dto.event.EventResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Event;
import com.example.nexuscore.model.EventRecipe;
import com.example.nexuscore.repository.EventRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private final EventRepository repository;

    public EventService(EventRepository repository) {
        this.repository = repository;
    }

    public List<EventResponse> list(Integer householdId) {
        return repository.findByHouseholdId(householdId).stream().map(this::toResponse).toList();
    }

    public EventResponse get(Integer householdId, String id) {
        return toResponse(findOwned(householdId, id));
    }

    public EventResponse create(Integer householdId, EventRequest request) {
        Event event = new Event(householdId, request.title(), request.description(), request.date(),
                request.duration(), request.location(), request.peopleCount());
        return toResponse(repository.save(event));
    }

    public EventResponse update(Integer householdId, String id, EventRequest request) {
        Event event = findOwned(householdId, id);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setDate(request.date());
        event.setDuration(request.duration());
        event.setLocation(request.location());
        event.setPeopleCount(request.peopleCount());
        return toResponse(repository.save(event));
    }

    public void remove(Integer householdId, String id) {
        repository.delete(findOwned(householdId, id));
    }

    public EventResponse linkRecipe(Integer householdId, String id, String recipeId) {
        Event event = findOwned(householdId, id);
        boolean alreadyLinked = event.getRecipes().stream().anyMatch(r -> r.getRecipeId().equals(recipeId));
        if (!alreadyLinked) {
            event.getRecipes().add(new EventRecipe(recipeId));
        }
        return toResponse(repository.save(event));
    }

    public EventResponse unlinkRecipe(Integer householdId, String id, String recipeId) {
        Event event = findOwned(householdId, id);
        event.getRecipes().removeIf(r -> r.getRecipeId().equals(recipeId));
        return toResponse(repository.save(event));
    }

    private Event findOwned(Integer householdId, String id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evento nao encontrado: " + id));
        if (!event.getHouseholdId().equals(householdId)) {
            throw new ForbiddenException("Evento nao pertence ao household autenticado");
        }
        return event;
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(), event.getHouseholdId(), event.getTitle(), event.getDescription(),
                event.getDate(), event.getDuration(), event.getLocation(), event.getPeopleCount(),
                event.getRecipes().stream().map(EventRecipe::getRecipeId).toList());
    }
}
