package com.punestartup.resume.entity;

import com.punestartup.candidate.entity.CandidateProfile;
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
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
public class CandidateResume {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(name = "storage_provider", nullable = false, length = 30)
    private String storageProvider;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private ResumeProcessingStatus processingStatus;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "consent_given_at", nullable = false)
    private Instant consentGivenAt;

    @Column(name = "consent_version", nullable = false, length = 50)
    private String consentVersion;

    @Column(name = "retention_expires_at", nullable = false)
    private Instant retentionExpiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "deleted_reason", length = 40)
    private ResumeDeletedReason deletedReason;

    @Column(name = "parse_error_code", length = 50)
    private String parseErrorCode;

    @Column(name = "parsed_skill_count", nullable = false)
    private int parsedSkillCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (uploadedAt == null) {
            uploadedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isExpired(Instant now) {
        return retentionExpiresAt != null && !retentionExpiresAt.isAfter(now);
    }
}
