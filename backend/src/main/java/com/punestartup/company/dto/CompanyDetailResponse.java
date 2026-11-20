package com.punestartup.company.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompanyDetailResponse(
        UUID id,
        String name,
        String slug,
        String websiteUrl,
        String logoUrl,
        String description,
        String industryName,
        String industrySlug,
        Short foundedYear,
        Integer employeeCountMin,
        Integer employeeCountMax,
        String fundingStage,
        String companyType,
        String verificationStatus,
        List<CompanyLocationResponse> locations,
        List<CompanySourceResponse> sources,
        long activeJobCount,
        long fresherJobCount,
        Instant createdAt,
        Instant updatedAt) {
}
