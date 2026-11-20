package com.punestartup.ingestion.dto;

import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ScrapingSourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateScrapingSourceRequest(
        @NotBlank @Size(max = 150) String sourceName,
        @NotNull ScrapingSourceType sourceType,
        @NotBlank @Size(max = 2048)
        @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String baseUrl,
        UUID companyId,
        PermissionReviewStatus permissionReviewStatus,
        @Size(max = 2000) String permissionNotes,
        @NotNull @Min(1) @Max(10080) Integer crawlIntervalMinutes,
        @Min(1) @Max(6000) Integer rateLimitPerMinute,
        Boolean enabled) {
}
