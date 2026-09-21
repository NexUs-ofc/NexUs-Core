package com.example.nexuscore.service;

import com.example.nexuscore.dto.event.EventRequest;
import com.example.nexuscore.dto.event.EventResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.model.Event;
import com.example.nexuscore.model.EventRecipe;
import com.example.nexuscore.model.Recipe;
import com.example.nexuscore.repository.EventRepository;
import com.example.nexuscore.repository.RecipeRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EventService {

    private final EventRepository repository;
    private final RecipeRepository recipeRepository;

    public EventService(EventRepository repository, RecipeRepository recipeRepository) {
        this.repository = repository;
        this.recipeRepository = recipeRepository;
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
        ObjectId objectId = parseId(recipeId, "Receita nao encontrada: ");
        Recipe recipe = recipeRepository.findById(objectId)
                .orElseThrow(() -> new NotFoundException("Receita nao encontrada: " + recipeId));
        boolean alreadyLinked = event.getRecipes().stream()
                .anyMatch(link -> link.getRecipeId().equals(objectId));
        if (!alreadyLinked) {
            event.getRecipes().add(new EventRecipe(objectId, recipe.getTitle()));
        }
        return toResponse(repository.save(event));
    }

    public EventResponse unlinkRecipe(Integer householdId, String id, String recipeId) {
        Event event = findOwned(householdId, id);
        ObjectId objectId = parseId(recipeId, "Receita nao encontrada: ");
        event.getRecipes().removeIf(recipe -> recipe.getRecipeId().equals(objectId));
        return toResponse(repository.save(event));
    }

    private Event findOwned(Integer householdId, String id) {
        ObjectId objectId = parseId(id, "Evento nao encontrado: ");
        Event event = repository.findById(objectId)
                .orElseThrow(() -> new NotFoundException("Evento nao encontrado: " + id));
        if (!event.getHouseholdId().equals(householdId)) {
            throw new ForbiddenException("Evento nao pertence ao household autenticado");
        }
        return event;
    }

    private ObjectId parseId(String id, String message) {
        if (!ObjectId.isValid(id)) {
            throw new NotFoundException(message + id);
        }
        return new ObjectId(id);
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId().toHexString(), event.getHouseholdId(), event.getTitle(), event.getDescription(),
                event.getDate(), event.getDuration(), event.getLocation(), event.getPeopleCount(),
                event.getRecipes().stream().map(recipe -> recipe.getRecipeId().toHexString()).toList());
    }
}
