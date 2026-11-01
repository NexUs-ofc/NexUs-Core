package com.example.nexuscore.repository;

import com.example.nexuscore.dto.notification.ProductNotificationCandidateProjection;
import com.example.nexuscore.model.PantryItem;
import com.example.nexuscore.model.ProfileStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PantryItemRepository extends JpaRepository<PantryItem, Integer> {

    @EntityGraph(attributePaths = "food")
    @Query("select item from PantryItem item where item.profile.id = :profileId and item.quantity > 0 "
            + "order by item.expiryDate asc, item.id desc")
    List<PantryItem> findByProfileIdOrderByExpiryDateAscIdDesc(@Param("profileId") Integer profileId);

    @EntityGraph(attributePaths = "food")
    @Query("select item from PantryItem item where item.profile.id = :profileId "
            + "and item.expiryDate < :date and item.quantity > 0")
    List<PantryItem> findByProfileIdAndExpiryDateBefore(@Param("profileId") Integer profileId,
                                                       @Param("date") LocalDate date);

    @EntityGraph(attributePaths = {"food", "profile", "profile.address"})
    @Query("select item from PantryItem item where item.expiryDate < :date and item.quantity > 0")
    Slice<PantryItem> findByExpiryDateBefore(@Param("date") LocalDate date, Pageable pageable);

    @EntityGraph(attributePaths = {"food", "profile", "profile.address"})
    @Query("select item from PantryItem item where item.expiryDate < :date "
            + "and item.profile.status = :status and item.quantity > 0")
    Slice<PantryItem> findExpiredByStatus(@Param("date") LocalDate date,
                                          @Param("status") ProfileStatus status, Pageable pageable);

    @Query(value = """
            select profile_id as "profileId", item_id as "itemId",
                   food_name as "foodName", expiry_date as "expiryDate"
            from fn_get_expiring_products(:fromDate, :toDate) candidate
            where exists (select 1 from pantry_item item where item.id = candidate.item_id and item.quantity > 0)
            order by expiry_date asc, item_id asc
            """, nativeQuery = true)
    Slice<ProductNotificationCandidateProjection> findExpiringProducts(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from PantryItem item where item.id = :id")
    Optional<PantryItem> findByIdForUpdate(@Param("id") Integer id);
}
