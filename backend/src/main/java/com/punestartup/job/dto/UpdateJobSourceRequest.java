package com.punestartup.job.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateJobSourceRequest(
        @Size(max = 150) String sourceName,
        @Size(max = 255) String sourceJobId,
        @Size(max = 2048)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "must be an http(s) URL") String sourceUrl,
        @Size(max = 2048)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "must be an http(s) URL") String applicationUrl,
        @Pattern(regexp = "DISCOVERED|ACTIVE|MISSING|REMOVED", message = "must be DISCOVERED, ACTIVE, MISSING or REMOVED")
        String sourceStatus) {
}
