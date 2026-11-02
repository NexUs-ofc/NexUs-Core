package com.example.nexuscore.controller;

import com.example.nexuscore.dto.plan.PlanResponse;
import com.example.nexuscore.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Planos")
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService service;

    public PlanController(PlanService service) {
        this.service = service;
    }

    @Operation(summary = "Listar planos disponíveis",
            description = "Catálogo público de planos ativos; não exige JWT.")
    @GetMapping
    @SecurityRequirements
    public List<PlanResponse> list() {
        return service.list();
    }
}
