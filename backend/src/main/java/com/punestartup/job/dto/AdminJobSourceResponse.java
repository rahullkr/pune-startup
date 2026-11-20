package com.punestartup.job.dto;

import com.punestartup.job.entity.JobSource;
import java.time.Instant;
import java.util.UUID;

public record AdminJobSourceResponse(
        UUID id,
        String sourceName,
        String sourceJobId,
        String sourceUrl,
        String applicationUrl,
        String sourceStatus,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastCheckedAt,
        int consecutiveMissingChecks,
        Instant removedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminJobSourceResponse from(JobSource source) {
        return new AdminJobSourceResponse(
                source.getId(),
                source.getSourceName(),
                source.getSourceJobId(),
                source.getSourceUrl(),
                source.getApplicationUrl(),
                source.getSourceStatus(),
                source.getFirstSeenAt(),
                source.getLastSeenAt(),
                source.getLastCheckedAt(),
                source.getConsecutiveMissingChecks(),
                source.getRemovedAt(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }
}
