package com.punestartup.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_sources")
@Getter
@Setter
@NoArgsConstructor
public class JobSource {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "source_job_id", length = 255)
    private String sourceJobId;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "application_url")
    private String applicationUrl;

    @Column(name = "source_status", nullable = false, length = 30)
    private String sourceStatus;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "consecutive_missing_checks", nullable = false)
    private int consecutiveMissingChecks;

    @Column(name = "scraping_source_id")
    private UUID scrapingSourceId;

    @Column(name = "last_missing_at")
    private Instant lastMissingAt;

    @Column(name = "last_missing_run_id")
    private UUID lastMissingRunId;

    @Column(name = "removed_at")
    private Instant removedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_metadata", columnDefinition = "jsonb")
    private String rawMetadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (firstSeenAt == null) {
            firstSeenAt = Instant.now();
        }
        if (lastSeenAt == null) {
            lastSeenAt = firstSeenAt;
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
