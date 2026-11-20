package com.punestartup.company.entity;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 220)
    private String normalizedName;

    @Column(name = "slug", nullable = false, length = 220)
    private String slug;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "website_domain", length = 253)
    private String websiteDomain;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id")
    private Industry industry;

    @Column(name = "founded_year")
    private Short foundedYear;

    @Column(name = "employee_count_min")
    private Integer employeeCountMin;

    @Column(name = "employee_count_max")
    private Integer employeeCountMax;

    @Column(name = "funding_stage", length = 50)
    private String fundingStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 40)
    private CompanyType companyType = CompanyType.STARTUP;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 20)
    private RecordStatus recordStatus = RecordStatus.ACTIVE;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<CompanyLocation> locations = new ArrayList<>();

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<CompanySource> sources = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
}
