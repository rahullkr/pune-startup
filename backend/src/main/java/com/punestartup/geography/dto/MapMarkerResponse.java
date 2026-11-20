package com.punestartup.geography.dto;

import java.util.UUID;

public record MapMarkerResponse(
        UUID companyId,
        String companyName,
        String companySlug,
        UUID locationId,
        String area,
        String city,
        double latitude,
        double longitude,
        long activeJobCount,
        long fresherJobCount) {
}
