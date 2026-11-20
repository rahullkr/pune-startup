package com.punestartup.ingestion.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobCompanyPayload(
        String name,
        String websiteUrl,
        String locationText) {
}
