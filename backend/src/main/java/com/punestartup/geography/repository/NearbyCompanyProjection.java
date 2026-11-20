package com.punestartup.geography.repository;

import java.util.UUID;

public interface NearbyCompanyProjection extends MapMarkerProjection {
    Double getDistanceMeters();
}
