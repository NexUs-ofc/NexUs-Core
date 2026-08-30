package com.example.nexuscore.controller;

import com.example.nexuscore.dto.event.EventRequest;
import com.example.nexuscore.dto.event.EventResponse;
import com.example.nexuscore.service.EventService;
import com.example.nexuscore.util.CurrentProfileResolver;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;
    private final CurrentProfileResolver currentProfile;

    public EventController(EventService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public List<EventResponse> list() {
        return service.list(currentProfile.profileId().intValue());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return service.create(currentProfile.profileId().intValue(), request);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable String id) {
        return service.get(currentProfile.profileId().intValue(), id);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable String id, @Valid @RequestBody EventRequest request) {
        return service.update(currentProfile.profileId().intValue(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        service.remove(currentProfile.profileId().intValue(), id);
    }

    @PostMapping("/{id}/recipes/{recipeId}")
    public EventResponse linkRecipe(@PathVariable String id, @PathVariable String recipeId) {
        return service.linkRecipe(currentProfile.profileId().intValue(), id, recipeId);
    }

    @DeleteMapping("/{id}/recipes/{recipeId}")
    public EventResponse unlinkRecipe(@PathVariable String id, @PathVariable String recipeId) {
        return service.unlinkRecipe(currentProfile.profileId().intValue(), id, recipeId);
    }
}
