package com.example.nexuscore.controller;

import com.example.nexuscore.dto.recipe.RecipeResponse;
import com.example.nexuscore.service.RecipeService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService service;
    private final CurrentProfileResolver currentProfile;

    public RecipeController(RecipeService service, CurrentProfileResolver currentProfile) {
        this.service = service;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public List<RecipeResponse> listSaved() {
        return service.listSaved(currentProfile.profileId().intValue());
    }

    @PostMapping("/{id}/favorite")
    public RecipeResponse favorite(@PathVariable String id) {
        return service.favorite(currentProfile.profileId().intValue(), id);
    }

    @DeleteMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavorite(@PathVariable String id) {
        service.unfavorite(currentProfile.profileId().intValue(), id);
    }
}
