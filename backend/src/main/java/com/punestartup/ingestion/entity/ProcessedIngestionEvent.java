package com.punestartup.ingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "processed_ingestion_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedIngestionEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
