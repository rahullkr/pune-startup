package com.punestartup.geography.repository;

import java.util.UUID;

public interface MapMarkerProjection {
    UUID getLocationId();
    UUID getCompanyId();
    String getCompanyName();
    String getCompanySlug();
    String getArea();
    String getCity();
    Double getLatitude();
    Double getLongitude();
}
