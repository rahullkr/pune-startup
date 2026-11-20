package com.punestartup.ingestion.entity;

import com.punestartup.company.entity.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "scraping_sources")
@Getter
@Setter
@NoArgsConstructor
public class ScrapingSource {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "source_name", nullable = false, length = 150)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private ScrapingSourceType sourceType;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_review_status", nullable = false, length = 30)
    private PermissionReviewStatus permissionReviewStatus = PermissionReviewStatus.PENDING;

    @Column(name = "permission_notes")
    private String permissionNotes;

    @Column(name = "crawl_interval_minutes", nullable = false)
    private int crawlIntervalMinutes;

    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "consecutive_failed_runs", nullable = false)
    private int consecutiveFailedRuns;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (permissionReviewStatus == null) {
            permissionReviewStatus = PermissionReviewStatus.PENDING;
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
