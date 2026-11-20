package com.punestartup.ingestion.dto;

import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.ingestion.entity.ScrapingSourceType;
import java.time.Instant;
import java.util.UUID;

public record ScrapingSourceResponse(
        UUID id,
        String sourceName,
        ScrapingSourceType sourceType,
        String baseUrl,
        UUID companyId,
        String companyName,
        PermissionReviewStatus permissionReviewStatus,
        String permissionNotes,
        int crawlIntervalMinutes,
        Integer rateLimitPerMinute,
        boolean enabled,
        Instant lastSuccessAt,
        int consecutiveFailedRuns,
        Instant lastFailureAt,
        Instant createdAt,
        Instant updatedAt) {

    public static ScrapingSourceResponse from(ScrapingSource source) {
        return new ScrapingSourceResponse(
                source.getId(),
                source.getSourceName(),
                source.getSourceType(),
                source.getBaseUrl(),
                source.getCompany() == null ? null : source.getCompany().getId(),
                source.getCompany() == null ? null : source.getCompany().getName(),
                source.getPermissionReviewStatus(),
                source.getPermissionNotes(),
                source.getCrawlIntervalMinutes(),
                source.getRateLimitPerMinute(),
                source.isEnabled(),
                source.getLastSuccessAt(),
                source.getConsecutiveFailedRuns(),
                source.getLastFailureAt(),
                source.getCreatedAt(),
                source.getUpdatedAt());
    }
}
