package com.example.nexuscore.repository;

import com.example.nexuscore.model.PantryProductSetting;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PantryProductSettingRepository extends JpaRepository<PantryProductSetting, Integer> {

    @EntityGraph(attributePaths = "food")
    List<PantryProductSetting> findByProfileId(Integer profileId);

    @Query("select setting from PantryProductSetting setting join fetch setting.food "
            + "where setting.profile.id = :profileId and "
            + "(select coalesce(sum(item.quantity), 0) from PantryItem item "
            + "where item.profile.id = :profileId and item.food.id = setting.food.id) "
            + "< setting.minimumQuantity")
    List<PantryProductSetting> findMissingByProfileId(@Param("profileId") Integer profileId);

    Optional<PantryProductSetting> findByProfileIdAndFoodId(Integer profileId, Integer foodId);
}
