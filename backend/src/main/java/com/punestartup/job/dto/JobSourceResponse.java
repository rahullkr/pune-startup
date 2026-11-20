package com.punestartup.job.dto;

import com.punestartup.job.entity.JobSource;
import java.time.Instant;

public record JobSourceResponse(
        String sourceName,
        String sourceUrl,
        String applicationUrl,
        String sourceStatus,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastCheckedAt) {

    public static JobSourceResponse from(JobSource source) {
        return new JobSourceResponse(
                source.getSourceName(),
                source.getSourceUrl(),
                source.getApplicationUrl(),
                source.getSourceStatus(),
                source.getFirstSeenAt(),
                source.getLastSeenAt(),
                source.getLastCheckedAt());
    }
}
