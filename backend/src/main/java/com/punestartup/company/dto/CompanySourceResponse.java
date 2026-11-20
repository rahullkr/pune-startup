package com.punestartup.company.dto;

import java.time.Instant;

public record CompanySourceResponse(
        String sourceName,
        String sourceUrl,
        String externalId,
        Instant lastVerifiedAt) {
}
