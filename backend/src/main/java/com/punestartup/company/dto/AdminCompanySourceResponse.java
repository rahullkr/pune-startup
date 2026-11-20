package com.punestartup.company.dto;

import com.punestartup.company.entity.CompanySource;
import java.time.Instant;

public record AdminCompanySourceResponse(
        String sourceName,
        String sourceUrl,
        String externalId,
        Instant lastVerifiedAt,
        String sourceMetadata,
        Instant createdAt) {

    public static AdminCompanySourceResponse from(CompanySource source) {
        return new AdminCompanySourceResponse(
                source.getSourceName(),
                source.getSourceUrl(),
                source.getExternalId(),
                source.getLastVerifiedAt(),
                source.getSourceMetadata(),
                source.getCreatedAt());
    }
}
