package com.punestartup.ingestion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScrapeSourcePayload(
        UUID sourceId,
        String sourceName,
        String sourceType,
        String baseUrl,
        UUID companyId,
        Integer rateLimitPerMinute) {
}
