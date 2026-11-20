package com.punestartup.job.dto;

import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.WorkMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateJobRequest(
        @NotNull UUID companyId,
        UUID companyLocationId,
        @NotBlank @Size(max = 250) String title,
        @Size(max = 20000) String description,
        EmploymentType employmentType,
        WorkMode workMode,
        @DecimalMin(value = "0.0", message = "must be non-negative")
        @Digits(integer = 2, fraction = 1) BigDecimal experienceMinYears,
        @DecimalMin(value = "0.0", message = "must be non-negative")
        @Digits(integer = 2, fraction = 1) BigDecimal experienceMaxYears,
        @DecimalMin(value = "0.0", message = "must be non-negative")
        @Digits(integer = 10, fraction = 2) BigDecimal salaryMin,
        @DecimalMin(value = "0.0", message = "must be non-negative")
        @Digits(integer = 10, fraction = 2) BigDecimal salaryMax,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a three-letter currency code")
        String salaryCurrency,
        @Size(max = 20) String salaryPeriod,
        Instant expiresAt,
        List<@Valid JobSkillInput> skills,
        @Valid JobSourceInput source) {
}
