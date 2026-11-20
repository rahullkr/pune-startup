package com.punestartup.job.dto;

import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.JobStatusHistory;
import java.time.Instant;
import java.util.UUID;

public record JobStatusHistoryResponse(
        UUID id,
        JobStatus oldStatus,
        JobStatus newStatus,
        String reason,
        UUID scrapingRunId,
        UUID changedByUserId,
        Instant changedAt) {

    public static JobStatusHistoryResponse from(JobStatusHistory history) {
        return new JobStatusHistoryResponse(
                history.getId(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getReason(),
                history.getScrapingRunId(),
                history.getChangedByUserId(),
                history.getChangedAt());
    }
}
