package com.punestartup.candidate.dto;

import com.punestartup.candidate.entity.ApplicationStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateApplicationRequest(
        ApplicationStatus status,
        Instant appliedAt,
        Boolean clearAppliedAt,
        @Size(max = 2048) String externalApplicationUrl,
        @Size(max = 20000) String notes,
        @Size(max = 1000) String statusNote) {
}
