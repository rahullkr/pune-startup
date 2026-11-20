package com.punestartup.company.dto;

import java.util.UUID;

public record CompanyLocationResponse(
        UUID id,
        String locationName,
        String addressLine,
        String area,
        String city,
        String state,
        String countryCode,
        String postalCode,
        String locationType,
        String geocodeStatus,
        boolean headquarters,
        Double latitude,
        Double longitude) {

    public static CompanyLocationResponse from(com.punestartup.company.entity.CompanyLocation location) {
        Double latitude = null;
        Double longitude = null;
        if (location.getLocation() != null) {
            latitude = location.getLocation().getY();
            longitude = location.getLocation().getX();
        }
        return new CompanyLocationResponse(
                location.getId(),
                location.getLocationName(),
                location.getAddressLine(),
                location.getArea(),
                location.getCity(),
                location.getState(),
                location.getCountryCode(),
                location.getPostalCode(),
                location.getLocationType() == null ? null : location.getLocationType().name(),
                location.getGeocodeStatus() == null ? null : location.getGeocodeStatus().name(),
                location.isHeadquarters(),
                latitude,
                longitude);
    }
}
