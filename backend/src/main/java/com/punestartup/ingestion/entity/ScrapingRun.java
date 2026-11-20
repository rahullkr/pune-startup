package com.punestartup.ingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scraping_runs")
@Getter
@Setter
@NoArgsConstructor
public class ScrapingRun {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scraping_source_id", nullable = false)
    private ScrapingSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ScrapingRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private ScrapingRunTrigger triggerType = ScrapingRunTrigger.MANUAL;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "records_discovered", nullable = false)
    private int recordsDiscovered;

    @Column(name = "records_emitted", nullable = false)
    private int recordsEmitted;

    @Column(name = "records_processed", nullable = false)
    private int recordsProcessed;

    @Column(name = "records_validated", nullable = false)
    private int recordsValidated;

    @Column(name = "records_rejected", nullable = false)
    private int recordsRejected;

    @Column(name = "records_duplicated", nullable = false)
    private int recordsDuplicated;

    @Column(name = "records_failed", nullable = false)
    private int recordsFailed;

    @Column(name = "error_summary")
    private String errorSummary;

    @Column(name = "freshness_reconciled_at")
    private Instant freshnessReconciledAt;

    @Column(name = "retried_from_run_id")
    private UUID retriedFromRunId;
}
