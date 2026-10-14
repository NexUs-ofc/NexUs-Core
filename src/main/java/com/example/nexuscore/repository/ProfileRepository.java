package com.example.nexuscore.repository;

import com.example.nexuscore.model.Profile;
import com.example.nexuscore.model.ProfileStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;

public interface ProfileRepository extends JpaRepository<Profile, Integer> {

    boolean existsByEmail(String email);

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
