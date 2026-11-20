package com.punestartup.company.dto;

import java.util.List;
import java.util.UUID;

public record CompanySummaryResponse(
        UUID id,
        String name,
        String slug,
        String industryName,
        String websiteUrl,
        String logoUrl,
        String fundingStage,
        Integer employeeCountMin,
        Integer employeeCountMax,
        String verificationStatus,
        List<String> areas,
        long activeJobCount,
        long fresherJobCount) {
}
