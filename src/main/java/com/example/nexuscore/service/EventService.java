package com.example.nexuscore.service;

import com.example.nexuscore.dto.event.EventRequest;
import com.example.nexuscore.dto.event.EventResponse;
import com.example.nexuscore.exception.ForbiddenException;
import com.example.nexuscore.exception.NotFoundException;
import com.example.nexuscore.mapper.EventMapper;
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
    private final ActivityRecordService activityRecordService;

    public EventService(EventRepository repository, RecipeRepository recipeRepository,
                        ActivityRecordService activityRecordService) {
        this.repository = repository;
        this.recipeRepository = recipeRepository;
        this.activityRecordService = activityRecordService;
    }
    public List<EventResponse> list(Integer householdId) {
        return repository.findByHouseholdId(householdId).stream().map(EventMapper::toResponse).toList();
    }

    public EventResponse get(Integer householdId, String id) {
        return EventMapper.toResponse(findOwned(householdId, id));
    }

    public EventResponse create(Integer householdId, EventRequest request) {
        Event event = new Event(householdId, request.title(), request.description(), request.date(),
                request.duration(), request.location(), request.peopleCount());
        Event saved = repository.save(event);
        activityRecordService.record(householdId, "event_created", "Criou o evento " + event.getTitle());
        return EventMapper.toResponse(saved);
    }

    public EventResponse update(Integer householdId, String id, EventRequest request) {
        Event event = findOwned(householdId, id);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setDate(request.date());
        event.setDuration(request.duration());
        event.setLocation(request.location());
        event.setPeopleCount(request.peopleCount());
        Event saved = repository.save(event);
        activityRecordService.record(householdId, "event_updated", "Atualizou o evento " + event.getTitle());
        return EventMapper.toResponse(saved);
    }

    public void remove(Integer householdId, String id) {
        Event event = findOwned(householdId, id);
        repository.delete(event);
        activityRecordService.record(householdId, "event_removed", "Removeu o evento " + event.getTitle());
    }

    public EventResponse linkRecipe(Integer householdId, String id, String recipeId) {
        Event event = findOwned(householdId, id);
        ObjectId objectId = parseId(recipeId, "Receita nao encontrada: ");
        Recipe recipe = recipeRepository.findByIdAndAccountId(objectId, householdId)
                .orElseThrow(() -> new NotFoundException("Receita nao encontrada: " + recipeId));
        boolean alreadyLinked = event.getRecipes().stream()
                .anyMatch(link -> link.getRecipeId().equals(objectId));
        if (!alreadyLinked) {
            event.getRecipes().add(new EventRecipe(objectId, recipe.getTitle()));
        }
        Event saved = repository.save(event);
        activityRecordService.record(householdId, "event_recipe_linked",
                "Associou a receita " + recipe.getTitle() + " ao evento " + event.getTitle());
        return EventMapper.toResponse(saved);
    }

    public EventResponse unlinkRecipe(Integer householdId, String id, String recipeId) {
        Event event = findOwned(householdId, id);
        ObjectId objectId = parseId(recipeId, "Receita nao encontrada: ");
        event.getRecipes().removeIf(recipe -> recipe.getRecipeId().equals(objectId));
        Event saved = repository.save(event);
        activityRecordService.record(householdId, "event_recipe_unlinked",
                "Removeu uma receita do evento " + event.getTitle());
        return EventMapper.toResponse(saved);
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

}
