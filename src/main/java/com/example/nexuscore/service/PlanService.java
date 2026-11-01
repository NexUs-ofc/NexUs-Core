package com.example.nexuscore.service;

import com.example.nexuscore.dto.plan.PlanResponse;
import com.example.nexuscore.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository plans;

    public PlanService(PlanRepository plans) {
        this.plans = plans;
    }

    public List<PlanResponse> list() {
        return plans.findByActiveTrueOrderByPlanPriceAscIdAsc().stream()
                .map(plan -> new PlanResponse(plan.getId(), plan.getPlanName(),
                        plan.getPlanPrice(), plan.getDescription()))
                .toList();
    }
}
