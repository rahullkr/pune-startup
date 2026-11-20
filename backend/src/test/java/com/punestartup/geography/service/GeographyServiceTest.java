package com.punestartup.geography.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.geography.repository.CompanyLocationSpatialRepository;
import com.punestartup.geography.repository.MapMarkerProjection;
import com.punestartup.geography.repository.MapMarkerProjectionTestFactory;
import com.punestartup.job.repository.CompanyJobCountProjection;
import com.punestartup.job.repository.JobRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeographyServiceTest {

    private CompanyLocationSpatialRepository spatialRepository;
    private JobRepository jobRepository;
    private GeographyService geographyService;

    @BeforeEach
    void setUp() {
        spatialRepository = mock(CompanyLocationSpatialRepository.class);
        jobRepository = mock(JobRepository.class);
        geographyService = new GeographyService(
                spatialRepository,
                jobRepository,
                new BigDecimal("1.0"),
                5.0,
                500,
                50000,
                50);
    }

    @Test
    void rejectsViewportWhereMinLatNotLessThanMaxLat() {
        assertThatThrownBy(() -> geographyService.getMapMarkers(18.5, 18.5, 73.7, 73.9))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("minLat must be less than maxLat");
    }

    @Test
    void rejectsViewportExceedingSpanLimit() {
        assertThatThrownBy(() -> geographyService.getMapMarkers(10.0, 20.0, 70.0, 73.9))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("viewport span");
    }

    @Test
    void rejectsOutOfRangeLongitude() {
        assertThatThrownBy(() -> geographyService.getMapMarkers(18.4, 18.6, -200.0, 73.9))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("minLng");
    }

    @Test
    void rejectsNegativeRadius() {
        assertThatThrownBy(() -> geographyService.getNearbyCompanies(18.5, 73.8, -1.0, null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("radiusKm");
    }

    @Test
    void rejectsRadiusAboveLimit() {
        assertThatThrownBy(() -> geographyService.getNearbyCompanies(18.5, 73.8, 100.0, null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("radiusKm");
    }

    @Test
    void rejectsLimitAboveMaximum() {
        assertThatThrownBy(() -> geographyService.getNearbyCompanies(18.5, 73.8, 5.0, 500))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void mapMarkersIncludeJobCounts() {
        UUID companyId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        MapMarkerProjection projection = MapMarkerProjectionTestFactory.create(
                locationId, companyId, "ZetaSoft", "zetasoft", "Hinjewadi", "Pune", 18.5913, 73.7275);
        when(spatialRepository.findMarkersInViewport(73.7, 18.5, 73.9, 18.7, 500))
                .thenReturn(List.of(projection));
        when(jobRepository.countActiveByCompanyIds(Mockito.anyCollection()))
                .thenReturn(List.of(new CompanyJobCountProjection() {
                    @Override
                    public UUID getCompanyId() {
                        return companyId;
                    }

                    @Override
                    public Long getJobCount() {
                        return 3L;
                    }
                }));
        when(jobRepository.countFresherEligibleByCompanyIds(Mockito.anyCollection(), Mockito.any()))
                .thenReturn(List.of());

        var markers = geographyService.getMapMarkers(18.5, 18.7, 73.7, 73.9);

        assertThat(markers).hasSize(1);
        assertThat(markers.get(0).companyId()).isEqualTo(companyId);
        assertThat(markers.get(0).companyName()).isEqualTo("ZetaSoft");
        assertThat(markers.get(0).activeJobCount()).isEqualTo(3);
        assertThat(markers.get(0).fresherJobCount()).isZero();
        assertThat(markers.get(0).latitude()).isEqualTo(18.5913);
        assertThat(markers.get(0).longitude()).isEqualTo(73.7275);
    }
}
