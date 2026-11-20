package com.punestartup.candidate.entity;

import com.punestartup.job.entity.Job;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saved_jobs")
@Getter
@Setter
@NoArgsConstructor
public class SavedJob {

    @EmbeddedId
    private SavedJobId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("candidateProfileId")
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("jobId")
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    @PrePersist
    void onCreate() {
        if (savedAt == null) {
            savedAt = Instant.now();
        }
    }
}
