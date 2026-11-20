package com.punestartup.candidate.dto;

import com.punestartup.candidate.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateApplicationRequest(
        @NotNull UUID jobId,
        ApplicationStatus status,
        Instant appliedAt,
        @Size(max = 2048) String externalApplicationUrl,
        @Size(max = 20000) String notes,
        @Size(max = 1000) String statusNote) {
}
