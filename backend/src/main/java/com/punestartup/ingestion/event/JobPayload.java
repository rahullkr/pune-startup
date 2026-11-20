package com.punestartup.ingestion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobPayload(
        String title,
        String description,
        String locationText,
        String employmentType,
        String workMode,
        BigDecimal experienceMinYears,
        BigDecimal experienceMaxYears,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String salaryPeriod,
        List<String> skills) {
}
