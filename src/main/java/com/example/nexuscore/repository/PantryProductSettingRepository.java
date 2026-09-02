package com.example.nexuscore.repository;

import com.example.nexuscore.model.PantryProductSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PantryProductSettingRepository extends JpaRepository<PantryProductSetting, Integer> {

    List<PantryProductSetting> findByProfileId(Integer profileId);

    Optional<PantryProductSetting> findByProfileIdAndFoodId(Integer profileId, Integer foodId);
}
