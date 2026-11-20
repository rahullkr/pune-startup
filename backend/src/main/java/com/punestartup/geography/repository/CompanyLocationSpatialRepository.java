package com.punestartup.geography.repository;

import com.punestartup.company.entity.CompanyLocation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyLocationSpatialRepository extends JpaRepository<CompanyLocation, UUID> {

    @Query(value = """
            SELECT cl.id AS locationId,
                   c.id AS companyId,
                   c.name AS companyName,
                   c.slug AS companySlug,
                   cl.area AS area,
                   cl.city AS city,
                   ST_Y(cl.location::geometry) AS latitude,
                   ST_X(cl.location::geometry) AS longitude
            FROM company_locations cl
            JOIN companies c ON c.id = cl.company_id
            WHERE c.record_status = 'ACTIVE'
              AND cl.geocode_status = 'VERIFIED'
              AND cl.location IS NOT NULL
              AND ST_Intersects(
                  cl.location::geometry,
                  ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
              )
            ORDER BY c.normalized_name
            LIMIT :maxResults
            """, nativeQuery = true)
    List<MapMarkerProjection> findMarkersInViewport(@Param("minLng") double minLng,
                                                    @Param("minLat") double minLat,
                                                    @Param("maxLng") double maxLng,
                                                    @Param("maxLat") double maxLat,
                                                    @Param("maxResults") int maxResults);

    @Query(value = """
            SELECT cl.id AS locationId,
                   c.id AS companyId,
                   c.name AS companyName,
                   c.slug AS companySlug,
                   cl.area AS area,
                   cl.city AS city,
                   ST_Y(cl.location::geometry) AS latitude,
                   ST_X(cl.location::geometry) AS longitude,
                   ST_Distance(
                       cl.location,
                       ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                   ) AS distanceMeters
            FROM company_locations cl
            JOIN companies c ON c.id = cl.company_id
            WHERE c.record_status = 'ACTIVE'
              AND cl.geocode_status = 'VERIFIED'
              AND cl.location IS NOT NULL
              AND ST_DWithin(
                  cl.location,
                  ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                  :radiusMeters
              )
            ORDER BY distanceMeters
            LIMIT :maxResults
            """, nativeQuery = true)
    List<NearbyCompanyProjection> findNearbyCompanies(@Param("latitude") double latitude,
                                                       @Param("longitude") double longitude,
                                                       @Param("radiusMeters") double radiusMeters,
                                                       @Param("maxResults") int maxResults);
}
