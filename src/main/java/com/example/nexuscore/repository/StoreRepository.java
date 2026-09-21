package com.example.nexuscore.repository;

import com.example.nexuscore.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Integer> {

    boolean existsByCnpj(String cnpj);

    boolean existsByProfileId(Integer profileId);

    @Query(value = """
            select * from (
                select
                    s.id as id,
                    p.name as name,
                    s.cnpj as cnpj,
                    a.street as street,
                    a.number as number,
                    a.neighborhood as neighborhood,
                    a.city as city,
                    a.state as state,
                    a.latitude as latitude,
                    a.longitude as longitude,
                    (6371 * acos(
                        cos(radians(:lat)) * cos(radians(a.latitude)) * cos(radians(a.longitude) - radians(:lng))
                        + sin(radians(:lat)) * sin(radians(a.latitude))
                    )) as distanceKm
                from store s
                join profile p on p.id = s.profile_id
                join address a on a.id = p.address_id
                where a.latitude is not null and a.longitude is not null
            ) ranked
            where distanceKm <= :radiusKm
            order by distanceKm asc
            """, nativeQuery = true)
    List<StoreDistanceProjection> findNearby(@Param("lat") double lat,
                                              @Param("lng") double lng,
                                              @Param("radiusKm") double radiusKm);
}
