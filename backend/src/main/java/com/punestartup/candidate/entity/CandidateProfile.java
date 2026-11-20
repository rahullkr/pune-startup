package com.punestartup.candidate.entity;

import com.punestartup.auth.entity.User;
import com.punestartup.job.entity.WorkMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidate_profiles")
@Getter
@Setter
@NoArgsConstructor
public class CandidateProfile {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "headline", length = 250)
    private String headline;

    @Column(name = "summary")
    private String summary;

    @Column(name = "total_experience_months")
    private Integer totalExperienceMonths;

    @Column(name = "preferred_city", length = 100)
    private String preferredCity;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_work_mode", length = 20)
    private WorkMode preferredWorkMode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
