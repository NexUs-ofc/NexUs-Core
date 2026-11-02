package com.example.nexuscore.repository;

import com.example.nexuscore.dto.stock.MissingStockProjection;
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

    @Query(value = """
            select missing.food_id as "foodId", missing.food_name as "foodName",
                   food.food_image_url as "foodImageUrl", missing.minimum_quantity as "minimumQuantity"
            from fn_get_items_below_minimum(:profileId) missing
            join food on food.id = missing.food_id
            order by missing.missing_quantity desc, missing.food_name, missing.food_id
            """, nativeQuery = true)
    List<MissingStockProjection> findMissingByProfileId(@Param("profileId") Integer profileId);

    Optional<PantryProductSetting> findByProfileIdAndFoodId(Integer profileId, Integer foodId);
}
