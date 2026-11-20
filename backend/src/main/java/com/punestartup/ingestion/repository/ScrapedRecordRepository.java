package com.punestartup.ingestion.repository;

import com.punestartup.ingestion.entity.ScrapedRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScrapedRecordRepository
        extends JpaRepository<ScrapedRecord, UUID>, JpaSpecificationExecutor<ScrapedRecord> {
}
