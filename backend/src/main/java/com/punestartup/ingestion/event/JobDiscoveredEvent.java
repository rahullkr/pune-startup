package com.punestartup.ingestion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobDiscoveredEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String producer,
        UUID runId,
        UUID sourceId,
        String sourceName,
        String sourceType,
        String externalJobId,
        String sourceUrl,
        String applicationUrl,
        Instant postedAt,
        JobCompanyPayload company,
        JobPayload job,
        JsonNode rawPayload) {
}
