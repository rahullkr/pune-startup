package com.punestartup.job.dto;

import com.punestartup.company.dto.CompanyLocationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JobSummaryResponse(
        UUID id,
        String title,
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
        boolean fresherEligible,
        JobCompanyResponse company,
        CompanyLocationResponse location) {
}
