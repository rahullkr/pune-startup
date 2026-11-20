package com.punestartup.candidate.dto;

import com.punestartup.candidate.entity.Application;
import com.punestartup.job.dto.JobSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record ApplicationSummaryResponse(
        UUID id,
        String status,
        Instant appliedAt,
        String externalApplicationUrl,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        JobSummaryResponse job) {

    public static ApplicationSummaryResponse from(Application application, JobSummaryResponse job) {
        return new ApplicationSummaryResponse(
                application.getId(),
                application.getStatus().name(),
                application.getAppliedAt(),
                application.getExternalApplicationUrl(),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                job);
    }
}
