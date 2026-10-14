package com.example.nexuscore.repository;

import com.example.nexuscore.dto.notification.ProductNotificationCandidateProjection;
import com.example.nexuscore.model.PantryItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface PantryItemRepository extends JpaRepository<PantryItem, Integer> {

    @EntityGraph(attributePaths = "food")
    List<PantryItem> findByProfileIdOrderByExpiryDateAscIdDesc(Integer profileId);

    @EntityGraph(attributePaths = "food")
    List<PantryItem> findByProfileIdAndExpiryDateBefore(Integer profileId, LocalDate date);

    @EntityGraph(attributePaths = {"food", "profile", "profile.address"})
    Slice<PantryItem> findByExpiryDateBefore(LocalDate date, Pageable pageable);

    @Query(value = """
            select profile_id as "profileId", item_id as "itemId",
                   food_name as "foodName", expiry_date as "expiryDate"
            from fn_get_expiring_products(:fromDate, :toDate)
            order by expiry_date asc, item_id asc
            """, nativeQuery = true)
    Slice<ProductNotificationCandidateProjection> findExpiringProducts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
