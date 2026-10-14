package com.example.nexuscore.repository;

import com.example.nexuscore.model.Food;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food, Integer> {

    @Override
    @EntityGraph(attributePaths = "category")
    List<Food> findAll();

    default List<Food> findByNameContainingIgnoreCase(String name) {
        String escaped = name.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return findByNamePattern("%" + escaped + "%");
    }

    @EntityGraph(attributePaths = "category")
    @Query("select food from Food food where lower(food.name) like lower(:pattern) escape '!'")
    List<Food> findByNamePattern(@Param("pattern") String pattern);

    Optional<Food> findByGtin(String gtin);
}
