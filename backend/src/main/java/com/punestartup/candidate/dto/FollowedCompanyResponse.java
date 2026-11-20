package com.punestartup.candidate.dto;

import java.time.Instant;
import java.util.UUID;

public record FollowedCompanyResponse(
        UUID companyId,
        Instant followedAt,
        boolean available,
        String name,
        String slug,
        String logoUrl,
        String websiteUrl,
        String industryName,
        String verificationStatus,
        String recordStatus,
        long activeJobCount) {
}
