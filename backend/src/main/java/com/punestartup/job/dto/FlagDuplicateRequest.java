package com.punestartup.job.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record FlagDuplicateRequest(
        @NotNull UUID duplicateOfJobId,
        @Size(max = 2000) String note) {
}
