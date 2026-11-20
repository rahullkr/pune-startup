package com.punestartup.geography.service;

import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.geography.dto.MapMarkerResponse;
import com.punestartup.geography.dto.NearbyCompanyResponse;
import com.punestartup.geography.repository.CompanyLocationSpatialRepository;
import com.punestartup.geography.repository.MapMarkerProjection;
import com.punestartup.geography.repository.NearbyCompanyProjection;
import com.punestartup.job.repository.CompanyJobCountProjection;
import com.punestartup.job.repository.JobRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeographyService {

    private final CompanyLocationSpatialRepository spatialRepository;
    private final JobRepository jobRepository;
    private final BigDecimal fresherMaxExperienceYears;
    private final double maxViewportSpanDegrees;
    private final int maxMapMarkers;
    private final double maxRadiusMeters;
    private final int maxNearbyResults;

    public GeographyService(CompanyLocationSpatialRepository spatialRepository,
                            JobRepository jobRepository,
                            @Value("${app.jobs.fresher-max-experience-years}") BigDecimal fresherMaxExperienceYears,
                            @Value("${app.map.max-viewport-span-degrees}") double maxViewportSpanDegrees,
                            @Value("${app.map.max-map-markers}") int maxMapMarkers,
                            @Value("${app.map.max-radius-meters}") double maxRadiusMeters,
                            @Value("${app.map.max-nearby-results}") int maxNearbyResults) {
        this.spatialRepository = spatialRepository;
        this.jobRepository = jobRepository;
        this.fresherMaxExperienceYears = fresherMaxExperienceYears;
        this.maxViewportSpanDegrees = maxViewportSpanDegrees;
        this.maxMapMarkers = maxMapMarkers;
        this.maxRadiusMeters = maxRadiusMeters;
        this.maxNearbyResults = maxNearbyResults;
    }

    @Transactional(readOnly = true)
    public List<MapMarkerResponse> getMapMarkers(double minLat, double maxLat, double minLng, double maxLng) {
        validateViewport(minLat, maxLat, minLng, maxLng);

        List<MapMarkerProjection> markers = spatialRepository.findMarkersInViewport(
                minLng, minLat, maxLng, maxLat, maxMapMarkers);
        if (markers.isEmpty()) {
            return List.of();
        }

        Set<UUID> companyIds = markers.stream().map(MapMarkerProjection::getCompanyId).collect(Collectors.toSet());
        Map<UUID, Long> activeCounts = toMap(jobRepository.countActiveByCompanyIds(companyIds));
        Map<UUID, Long> fresherCounts =
                toMap(jobRepository.countFresherEligibleByCompanyIds(companyIds, fresherMaxExperienceYears));

        return markers.stream()
                .map(marker -> new MapMarkerResponse(
                        marker.getCompanyId(),
                        marker.getCompanyName(),
                        marker.getCompanySlug(),
                        marker.getLocationId(),
                        marker.getArea(),
                        marker.getCity(),
                        marker.getLatitude(),
                        marker.getLongitude(),
                        activeCounts.getOrDefault(marker.getCompanyId(), 0L),
                        fresherCounts.getOrDefault(marker.getCompanyId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NearbyCompanyResponse> getNearbyCompanies(double latitude, double longitude, double radiusKm,
                                                         Integer limit) {
        validateLatitude("lat", latitude);
        validateLongitude("lng", longitude);
        if (radiusKm <= 0 || radiusKm * 1000 > maxRadiusMeters) {
            throw InvalidParameterException.of("radiusKm",
                    "must be greater than 0 and at most %s km".formatted(maxRadiusMeters / 1000));
        }
        int effectiveLimit = resolveLimit(limit);

        List<NearbyCompanyProjection> nearby = spatialRepository.findNearbyCompanies(
                latitude, longitude, radiusKm * 1000, effectiveLimit);
        return nearby.stream()
                .map(row -> new NearbyCompanyResponse(
                        row.getCompanyId(),
                        row.getCompanyName(),
                        row.getCompanySlug(),
                        row.getLocationId(),
                        row.getArea(),
                        row.getCity(),
                        row.getLatitude(),
                        row.getLongitude(),
                        row.getDistanceMeters()))
                .toList();
    }

    private void validateViewport(double minLat, double maxLat, double minLng, double maxLng) {
        validateLatitude("minLat", minLat);
        validateLatitude("maxLat", maxLat);
        validateLongitude("minLng", minLng);
        validateLongitude("maxLng", maxLng);
        if (minLat >= maxLat) {
            throw InvalidParameterException.of("minLat/maxLat", "minLat must be less than maxLat");
        }
        if (minLng >= maxLng) {
            throw InvalidParameterException.of("minLng/maxLng", "minLng must be less than maxLng");
        }
        if (maxLat - minLat > maxViewportSpanDegrees || maxLng - minLng > maxViewportSpanDegrees) {
            throw InvalidParameterException.of("viewport",
                    "viewport span must not exceed %s degrees".formatted(maxViewportSpanDegrees));
        }
    }

    private void validateLatitude(String name, double value) {
        if (!Double.isFinite(value) || value < -90 || value > 90) {
            throw InvalidParameterException.of(name, "must be a finite number between -90 and 90");
        }
    }

    private void validateLongitude(String name, double value) {
        if (!Double.isFinite(value) || value < -180 || value > 180) {
            throw InvalidParameterException.of(name, "must be a finite number between -180 and 180");
        }
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return maxNearbyResults;
        }
        if (limit <= 0 || limit > maxNearbyResults) {
            throw InvalidParameterException.of("limit",
                    "must be between 1 and %d".formatted(maxNearbyResults));
        }
        return limit;
    }

    private Map<UUID, Long> toMap(List<CompanyJobCountProjection> counts) {
        return counts.stream().collect(Collectors.toMap(CompanyJobCountProjection::getCompanyId,
                CompanyJobCountProjection::getJobCount));
    }
}
