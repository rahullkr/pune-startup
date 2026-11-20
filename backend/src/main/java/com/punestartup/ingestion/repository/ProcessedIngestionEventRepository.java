package com.punestartup.ingestion.repository;

import com.punestartup.ingestion.entity.ProcessedIngestionEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedIngestionEventRepository extends JpaRepository<ProcessedIngestionEvent, UUID> {
}
