package com.example.nexuscore.repository;

import com.example.nexuscore.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByCategoryName(String categoryName);
}
