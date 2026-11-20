package com.punestartup.job.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MergeJobsRequest(
        @NotNull UUID duplicateJobId,
        @Size(max = 2000) String note) {
}
