package com.example.nexuscore.repository;

import com.example.nexuscore.model.Food;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Integer> {

    @Override
    @EntityGraph(attributePaths = "category")
    List<Food> findAll();

    @EntityGraph(attributePaths = "category")
    List<Food> findByNameContainingIgnoreCase(String name);

    Optional<Food> findByGtin(String gtin);
}
