package com.example.nexuscore.repository;

import com.example.nexuscore.model.PantryItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PantryItemRepository extends JpaRepository<PantryItem, Integer> {

    @EntityGraph(attributePaths = "food")
    List<PantryItem> findByProfileIdOrderByExpiryDateAscIdDesc(Integer profileId);

    @EntityGraph(attributePaths = "food")
    List<PantryItem> findByProfileIdAndExpiryDateBefore(Integer profileId, LocalDate date);
}
