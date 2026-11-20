package com.punestartup.ingestion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScrapeRunCompletedEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String producer,
        UUID runId,
        UUID sourceId,
        String status,
        int recordsFetched,
        int recordsEmitted,
        String errorSummary) {
}
