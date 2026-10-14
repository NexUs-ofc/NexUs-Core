package com.example.nexuscore.repository;

import com.example.nexuscore.dto.notification.NearbyHouseholdProjection;
import com.example.nexuscore.dto.notification.NearbyStorePairProjection;
import com.example.nexuscore.dto.notification.NewStoreProjection;
import com.example.nexuscore.dto.store.StoreDistanceProjection;
import com.example.nexuscore.model.ProfileStatus;
import com.example.nexuscore.model.Store;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Integer> {

    @Override
    @EntityGraph(attributePaths = {"profile", "profile.address"})
    Optional<Store> findById(Integer id);

    @EntityGraph(attributePaths = {"profile", "profile.address", "company", "company.profile"})
    @Query("select s from Store s where s.id = :id and s.profile.status = :storeStatus "
            + "and s.company.profile.status = :companyStatus")
    Optional<Store> findByIdForActiveProfiles(@Param("id") Integer id,
                                              @Param("storeStatus") ProfileStatus storeStatus,
                                              @Param("companyStatus") ProfileStatus companyStatus);

    @Override
    @EntityGraph(attributePaths = {"profile", "profile.address"})
    List<Store> findAll();

    @EntityGraph(attributePaths = {"profile", "profile.address", "company", "company.profile"})
    @Query("select s from Store s where s.profile.status = :storeStatus "
            + "and s.company.profile.status = :companyStatus")
    List<Store> findAllForActiveProfiles(@Param("storeStatus") ProfileStatus storeStatus,
                                         @Param("companyStatus") ProfileStatus companyStatus);

    boolean existsByCnpj(String cnpj);

    boolean existsByProfileId(Integer profileId);

    boolean existsByCnpjAndIdNot(String cnpj, Integer id);

    long countByCompanyId(Integer companyId);

    @Query(value = """
            select s.id as "storeId", sp.name as "storeName", sp.created_at as "createdAt"
            from store s
            join profile sp on sp.id = s.profile_id
            join company c on c.id = s.company_id
            join profile cp on cp.id = c.profile_id
            where sp.status = 'ACTIVE'
                and cp.status = 'ACTIVE'
                and sp.created_at >= :fromDate
                and sp.created_at < :untilDate
            order by sp.created_at asc, s.id asc
            """, nativeQuery = true)
    Slice<NewStoreProjection> findNewActiveStores(@Param("fromDate") LocalDateTime fromDate,
                                                  @Param("untilDate") LocalDateTime untilDate,
                                                  Pageable pageable);

    @Query(value = """
            select h.id as "profileId", s.id as "storeId", sp.name as "storeName",
                ST_Distance(
                    ST_SetSRID(ST_MakePoint(ha.longitude::double precision,
                        ha.latitude::double precision), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(sa.longitude::double precision,
                        sa.latitude::double precision), 4326)::geography
                ) / 1000.0 as "distanceKm"
            from profile h
            join address ha on ha.id = h.address_id
            cross join store s
            join profile sp on sp.id = s.profile_id
            join address sa on sa.id = sp.address_id
            join company c on c.id = s.company_id
            join profile cp on cp.id = c.profile_id
            where h.id in :profileIds
                and h.type = 'HOUSEHOLD'
                and h.status = 'ACTIVE'
                and sp.type = 'STORE'
                and sp.status = 'ACTIVE'
                and cp.status = 'ACTIVE'
                and ha.latitude is not null and ha.longitude is not null
                and sa.latitude is not null and sa.longitude is not null
                and ST_DWithin(
                    ST_SetSRID(ST_MakePoint(ha.longitude::double precision,
                        ha.latitude::double precision), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(sa.longitude::double precision,
                        sa.latitude::double precision), 4326)::geography,
                    :radiusKm * 1000.0
                )
            order by h.id, "distanceKm", s.id
            """, nativeQuery = true)
    List<NearbyStorePairProjection> findNearbyStoresForProfiles(@Param("profileIds") List<Integer> profileIds,
                                                                 @Param("radiusKm") double radiusKm);

    @Query(value = """
            select h.id as "profileId"
            from store s
            join profile sp on sp.id = s.profile_id
            join address sa on sa.id = sp.address_id
            join company c on c.id = s.company_id
            join profile cp on cp.id = c.profile_id
            cross join profile h
            join address ha on ha.id = h.address_id
            where s.id = :storeId
                and h.type = 'HOUSEHOLD'
                and h.status = 'ACTIVE'
                and sp.status = 'ACTIVE'
                and cp.status = 'ACTIVE'
                and ha.latitude is not null and ha.longitude is not null
                and sa.latitude is not null and sa.longitude is not null
                and ST_DWithin(
                    ST_SetSRID(ST_MakePoint(ha.longitude::double precision,
                        ha.latitude::double precision), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(sa.longitude::double precision,
                        sa.latitude::double precision), 4326)::geography,
                    :radiusKm * 1000.0
                )
            order by h.id
            """, nativeQuery = true)
    List<NearbyHouseholdProjection> findActiveHouseholdsNearStore(@Param("storeId") Integer storeId,
                                                                   @Param("radiusKm") double radiusKm);

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
                    s.opening_time as openingTime,
                    s.closing_time as closingTime,
                    (ST_Distance(
                    ST_SetSRID(
                        ST_MakePoint(a.longitude::double precision, a.latitude::double precision), 4326
                    )::geography,
                        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                    ) / 1000.0) as distanceKm,
                    a.street as street,
                    a.number as number,
                    a.neighborhood as neighborhood,
                    a.city as city,
                    a.state as state,
                    a.latitude as latitude,
                    a.longitude as longitude
                from store s
                join profile p on p.id = s.profile_id
                join address a on a.id = p.address_id
            join company c on c.id = s.company_id
            join profile cp on cp.id = c.profile_id
                where a.latitude is not null and a.longitude is not null
                    and p.status = 'ACTIVE'
                    and cp.status = 'ACTIVE'
                    and ST_DWithin(
                        ST_SetSRID(
                            ST_MakePoint(a.longitude::double precision, a.latitude::double precision), 4326
                        )::geography,
                        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                        :radiusKm * 1000.0
                    )
            ) ranked
            order by distanceKm asc
            """, nativeQuery = true)
    List<StoreDistanceProjection> findNearby(@Param("lat") double lat,
                                              @Param("lng") double lng,
                                              @Param("radiusKm") double radiusKm);
}
