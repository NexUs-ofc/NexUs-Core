package com.example.nexuscore.repository;

import com.example.nexuscore.dto.store.StoreDistanceProjection;
import com.example.nexuscore.model.Store;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Integer> {

    @Override
    @EntityGraph(attributePaths = {"profile", "profile.address"})
    Optional<Store> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"profile", "profile.address"})
    List<Store> findAll();

    boolean existsByCnpj(String cnpj);

    boolean existsByProfileId(Integer profileId);

    boolean existsByCnpjAndIdNot(String cnpj, Integer id);

    long countByCompanyId(Integer companyId);

    @EntityGraph(attributePaths = {"profile", "profile.address"})
    List<Store> findByCompanyId(Integer companyId);

    @EntityGraph(attributePaths = {"profile", "profile.address"})
    Optional<Store> findByIdAndCompanyId(Integer id, Integer companyId);

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
