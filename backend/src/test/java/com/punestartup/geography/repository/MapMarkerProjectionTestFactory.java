package com.punestartup.geography.repository;

import java.util.UUID;

public final class MapMarkerProjectionTestFactory {

    private MapMarkerProjectionTestFactory() {
    }

    public static MapMarkerProjection create(UUID locationId, UUID companyId, String companyName,
                                             String companySlug, String area, String city,
                                             double latitude, double longitude) {
        return new MapMarkerProjection() {
            @Override
            public UUID getLocationId() {
                return locationId;
            }

            @Override
            public UUID getCompanyId() {
                return companyId;
            }

            @Override
            public String getCompanyName() {
                return companyName;
            }

            @Override
            public String getCompanySlug() {
                return companySlug;
            }

            @Override
            public String getArea() {
                return area;
            }

            @Override
            public String getCity() {
                return city;
            }

            @Override
            public Double getLatitude() {
                return latitude;
            }

            @Override
            public Double getLongitude() {
                return longitude;
            }
        };
    }
}
