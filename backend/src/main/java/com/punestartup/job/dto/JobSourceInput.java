package com.punestartup.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JobSourceInput(
        @NotBlank @Size(max = 150) String sourceName,
        @Size(max = 255) String sourceJobId,
        @NotBlank @Size(max = 2048)
        @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL") String sourceUrl,
        @Size(max = 2048)
        @Pattern(regexp = "^$|^https?://\\S+$", message = "must be an http(s) URL") String applicationUrl) {
}
