package com.example.nexuscore.repository;

import com.example.nexuscore.model.PantryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface PantryItemRepository extends JpaRepository<PantryItem, Integer> {

    List<PantryItem> findByProfileIdOrderByExpiryDateAscIdDesc(Integer profileId);

    List<PantryItem> findByProfileIdAndExpiryDateBefore(Integer profileId, LocalDate date);

    @Query("select coalesce(sum(p.quantity), 0) from PantryItem p"
            + " where p.profile.id = :profileId and p.food.id = :foodId")
    Integer sumQuantityByProfileIdAndFoodId(
            @Param("profileId") Integer profileId, @Param("foodId") Integer foodId);
}
