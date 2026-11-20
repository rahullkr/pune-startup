package com.punestartup.ingestion.entity;

import com.punestartup.job.entity.Job;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scraped_records")
@Getter
@Setter
@NoArgsConstructor
public class ScrapedRecord {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scraping_run_id", nullable = false)
    private ScrapingRun run;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 30)
    private ScrapedRecordType recordType;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "source_url")
    private String sourceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ScrapedRecordStatus processingStatus;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "event_id")
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "error_message")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_payload", columnDefinition = "jsonb")
    private String normalizedPayload;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
