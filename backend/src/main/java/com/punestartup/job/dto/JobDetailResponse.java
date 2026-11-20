package com.punestartup.job.dto;

import com.punestartup.company.dto.CompanyLocationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobDetailResponse(
        UUID id,
        String title,
        String description,
        String employmentType,
        String workMode,
        BigDecimal experienceMinYears,
        BigDecimal experienceMaxYears,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String salaryPeriod,
        String status,
        Instant postedAt,
        Instant firstPublishedAt,
        Instant lastVerifiedAt,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastCheckedAt,
        Instant expiresAt,
        boolean fresherEligible,
        JobCompanyResponse company,
        CompanyLocationResponse location,
        List<JobSkillResponse> skills,
        List<JobSourceResponse> sources,
        Instant createdAt,
        Instant updatedAt) {
}
