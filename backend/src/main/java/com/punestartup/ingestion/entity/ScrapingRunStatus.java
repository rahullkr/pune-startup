package com.punestartup.ingestion.entity;

public enum ScrapingRunStatus {
    RUNNING,
    SUCCEEDED,
    PARTIAL,
    FAILED,
    SKIPPED,
    MISSED
}
