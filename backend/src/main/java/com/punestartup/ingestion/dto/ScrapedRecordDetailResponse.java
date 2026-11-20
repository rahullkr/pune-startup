package com.punestartup.ingestion.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.ingestion.entity.ScrapedRecord;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.entity.ScrapedRecordType;
import com.punestartup.job.entity.JobStatus;
import java.time.Instant;
import java.util.UUID;

public record ScrapedRecordDetailResponse(
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
        Instant processedAt,
        JsonNode payload,
        JsonNode normalizedPayload) {

    public static ScrapedRecordDetailResponse from(ScrapedRecord record, ObjectMapper objectMapper) {
        return new ScrapedRecordDetailResponse(
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
                record.getProcessedAt(),
                readTree(objectMapper, record.getPayload()),
                readTree(objectMapper, record.getNormalizedPayload()));
    }

    private static JsonNode readTree(ObjectMapper objectMapper, String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("Stored ingestion payload is not valid JSON", ex);
        }
    }
}
