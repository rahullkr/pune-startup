package com.punestartup.ingestion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScrapeSourceRequestedCommand(
        UUID commandId,
        String commandType,
        int schemaVersion,
        Instant requestedAt,
        UUID runId,
        ScrapeSourcePayload source) {
}
