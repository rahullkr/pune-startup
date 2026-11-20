package com.punestartup.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "company_sources")
@Getter
@Setter
@NoArgsConstructor
public class CompanySource {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_metadata", columnDefinition = "jsonb")
    private String sourceMetadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
