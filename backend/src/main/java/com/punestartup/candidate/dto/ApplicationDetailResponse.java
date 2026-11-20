package com.punestartup.candidate.dto;

import com.punestartup.job.dto.JobSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApplicationDetailResponse(
        UUID id,
        String status,
        Instant appliedAt,
        String externalApplicationUrl,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        JobSummaryResponse job,
        List<ApplicationStatusHistoryResponse> history) {
}
