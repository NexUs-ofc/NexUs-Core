package com.example.nexuscore.repository;

import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profile p where p.id = :id")
    Optional<Profile> findForImageUpdate(@Param("id") Integer id);

    List<Profile> findByIdInAndStatus(Collection<Integer> ids, ProfileStatus status);

    @Query(value = """
            select p.*
            from profile p
            join address a on a.id = p.address_id
            where p.type = 'HOUSEHOLD'
                and p.status = 'ACTIVE'
                and a.latitude is not null
                and a.longitude is not null
            order by p.id
            """, nativeQuery = true)
    Slice<Profile> findActiveGeocodedHouseholds(Pageable pageable);
}
