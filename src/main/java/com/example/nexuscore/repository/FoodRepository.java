package com.example.nexuscore.repository;

import com.example.nexuscore.model.Food;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodRepository extends JpaRepository<Food, Integer> {

    List<Food> findByNameContainingIgnoreCase(String name);
}
