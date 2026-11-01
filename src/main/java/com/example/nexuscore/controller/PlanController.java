package com.example.nexuscore.controller;

import com.example.nexuscore.dto.plan.PlanResponse;
import com.example.nexuscore.service.PlanService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @GetMapping
    @SecurityRequirements
    public List<PlanResponse> list() {
        return service.list();
    }
}
