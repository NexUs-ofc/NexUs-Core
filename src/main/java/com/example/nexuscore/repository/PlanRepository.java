package com.example.nexuscore.repository;

import com.example.nexuscore.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Integer> {
    List<Plan> findByActiveTrueOrderByPlanPriceAscIdAsc();

    Optional<Plan> findByIdAndActiveTrue(Integer id);
}
