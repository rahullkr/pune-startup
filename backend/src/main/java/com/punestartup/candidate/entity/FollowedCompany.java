package com.punestartup.candidate.entity;

import com.punestartup.company.entity.Company;
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
@Table(name = "followed_companies")
@Getter
@Setter
@NoArgsConstructor
public class FollowedCompany {

    @EmbeddedId
    private FollowedCompanyId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("candidateProfileId")
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("companyId")
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "followed_at", nullable = false)
    private Instant followedAt;

    @PrePersist
    void onCreate() {
        if (followedAt == null) {
            followedAt = Instant.now();
        }
    }
}
