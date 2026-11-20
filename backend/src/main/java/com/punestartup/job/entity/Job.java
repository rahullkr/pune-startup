package com.punestartup.job.entity;

import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class Job {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_location_id")
    private CompanyLocation companyLocation;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "normalized_title", nullable = false, length = 250)
    private String normalizedTitle;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;

    @Column(name = "experience_min_years", precision = 4, scale = 1)
    private BigDecimal experienceMinYears;

    @Column(name = "experience_max_years", precision = 4, scale = 1)
    private BigDecimal experienceMaxYears;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency;

    @Column(name = "salary_period", length = 20)
    private String salaryPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "first_published_at")
    private Instant firstPublishedAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "stale_since")
    private Instant staleSince;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    private List<JobSkill> skills = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isFresherEligible(BigDecimal fresherMaxExperienceYears) {
        return experienceMinYears == null || experienceMinYears.compareTo(fresherMaxExperienceYears) <= 0;
    }
}
