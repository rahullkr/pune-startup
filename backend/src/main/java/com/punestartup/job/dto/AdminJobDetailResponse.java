package com.punestartup.job.dto;

import com.punestartup.company.dto.CompanyLocationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminJobDetailResponse(
        UUID id,
        String title,
        String description,
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
        Instant firstPublishedAt,
        Instant lastVerifiedAt,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastCheckedAt,
        Instant staleSince,
        Instant expiresAt,
        Instant expiredAt,
        JobCompanyResponse company,
        CompanyLocationResponse location,
        List<JobSkillResponse> skills,
        List<AdminJobSourceResponse> sources,
        List<JobStatusHistoryResponse> history,
        Instant createdAt,
        Instant updatedAt) {
}
