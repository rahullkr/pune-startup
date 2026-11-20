package com.punestartup.job.entity;

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

@Entity
@Table(name = "job_status_history")
@Getter
@Setter
@NoArgsConstructor
public class JobStatusHistory {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private JobStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private JobStatus newStatus;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(name = "scraping_run_id")
    private UUID scrapingRunId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
