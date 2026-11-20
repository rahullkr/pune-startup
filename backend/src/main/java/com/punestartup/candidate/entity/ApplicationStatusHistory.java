package com.punestartup.candidate.entity;

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
@Table(name = "application_status_history")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationStatusHistory {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private ApplicationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ApplicationStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "note")
    private String note;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
