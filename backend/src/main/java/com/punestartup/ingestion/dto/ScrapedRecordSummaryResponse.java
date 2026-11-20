package com.punestartup.ingestion.dto;

import com.punestartup.ingestion.entity.ScrapedRecord;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.entity.ScrapedRecordType;
import com.punestartup.job.entity.JobStatus;
import java.time.Instant;
import java.util.UUID;

public record ScrapedRecordSummaryResponse(
        UUID id,
        UUID runId,
        UUID sourceId,
        String sourceName,
        ScrapedRecordType recordType,
        String externalId,
        String sourceUrl,
        ScrapedRecordStatus processingStatus,
        UUID jobId,
        JobStatus jobStatus,
        String errorMessage,
        Instant createdAt,
        Instant processedAt) {

    public static ScrapedRecordSummaryResponse from(ScrapedRecord record) {
        return new ScrapedRecordSummaryResponse(
                record.getId(),
                record.getRun().getId(),
                record.getRun().getSource().getId(),
                record.getRun().getSource().getSourceName(),
                record.getRecordType(),
                record.getExternalId(),
                record.getSourceUrl(),
                record.getProcessingStatus(),
                record.getJob() == null ? null : record.getJob().getId(),
                record.getJob() == null ? null : record.getJob().getStatus(),
                record.getErrorMessage(),
                record.getCreatedAt(),
                record.getProcessedAt());
    }
}
