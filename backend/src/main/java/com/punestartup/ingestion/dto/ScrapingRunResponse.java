package com.punestartup.ingestion.dto;

import com.punestartup.ingestion.entity.ScrapingRun;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import com.punestartup.ingestion.entity.ScrapingRunTrigger;
import java.time.Instant;
import java.util.UUID;

public record ScrapingRunResponse(
        UUID id,
        UUID sourceId,
        String sourceName,
        ScrapingRunStatus status,
        ScrapingRunTrigger triggerType,
        Instant startedAt,
        Instant finishedAt,
        int recordsDiscovered,
        int recordsEmitted,
        int recordsProcessed,
        int recordsValidated,
        int recordsAccepted,
        int recordsRejected,
        int recordsDuplicated,
        int recordsFailed,
        String errorSummary,
        UUID retriedFromRunId) {

    public static ScrapingRunResponse from(ScrapingRun run) {
        return new ScrapingRunResponse(
                run.getId(),
                run.getSource().getId(),
                run.getSource().getSourceName(),
                run.getStatus(),
                run.getTriggerType(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getRecordsDiscovered(),
                run.getRecordsEmitted(),
                run.getRecordsProcessed(),
                run.getRecordsValidated(),
                run.getRecordsValidated() + run.getRecordsDuplicated(),
                run.getRecordsRejected(),
                run.getRecordsDuplicated(),
                run.getRecordsFailed(),
                run.getErrorSummary(),
                run.getRetriedFromRunId());
    }
}
