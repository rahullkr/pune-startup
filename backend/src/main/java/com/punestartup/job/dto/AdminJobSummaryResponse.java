package com.punestartup.job.dto;

import com.punestartup.company.dto.CompanyLocationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminJobSummaryResponse(
        UUID id,
        String title,
        String status,
        String employmentType,
        String workMode,
        BigDecimal experienceMinYears,
        BigDecimal experienceMaxYears,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String salaryPeriod,
        Instant postedAt,
        Instant lastVerifiedAt,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastCheckedAt,
        Instant expiresAt,
        JobCompanyResponse company,
        CompanyLocationResponse location,
        Instant createdAt,
        Instant updatedAt) {
}
