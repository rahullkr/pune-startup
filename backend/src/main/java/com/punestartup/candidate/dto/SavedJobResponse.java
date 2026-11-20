package com.punestartup.candidate.dto;

import com.punestartup.job.dto.JobSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record SavedJobResponse(
        UUID jobId,
        Instant savedAt,
        boolean available,
        JobSummaryResponse job) {
}
