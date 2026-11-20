package com.punestartup.company.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.locationtech.jts.geom.Point;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "company_locations")
@Getter
@Setter
@NoArgsConstructor
public class CompanyLocation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "location_name", length = 150)
    private String locationName;

    @Column(name = "address_line")
    private String addressLine;

    @Column(name = "area", length = 120)
    private String area;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "postal_code", length = 15)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 30)
    private LocationType locationType;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(name = "location", columnDefinition = "geography(POINT, 4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "geocode_status", nullable = false, length = 30)
    private GeocodeStatus geocodeStatus;

    @Column(name = "is_headquarters", nullable = false)
    private boolean headquarters;

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
