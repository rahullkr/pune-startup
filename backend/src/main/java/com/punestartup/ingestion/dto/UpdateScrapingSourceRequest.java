package com.punestartup.ingestion.dto;

import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ScrapingSourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateScrapingSourceRequest(
        @Size(min = 1, max = 150) String sourceName,
        ScrapingSourceType sourceType,
        @Size(max = 2048)
        @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String baseUrl,
        UUID companyId,
        PermissionReviewStatus permissionReviewStatus,
        @Size(max = 2000) String permissionNotes,
        @Min(1) @Max(10080) Integer crawlIntervalMinutes,
        @Min(1) @Max(6000) Integer rateLimitPerMinute,
        Boolean enabled) {
}
