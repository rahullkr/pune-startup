package com.punestartup.ingestion.repository;

import com.punestartup.ingestion.entity.ScrapingRun;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScrapingRunRepository
        extends JpaRepository<ScrapingRun, UUID>, JpaSpecificationExecutor<ScrapingRun> {

    boolean existsBySource_IdAndStatus(UUID sourceId, ScrapingRunStatus status);

    Optional<ScrapingRun> findFirstBySource_IdOrderByStartedAtDesc(UUID sourceId);

    Optional<ScrapingRun> findFirstBySource_IdAndStatusInOrderByStartedAtDesc(
            UUID sourceId, Collection<ScrapingRunStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ScrapingRun r where r.id = :id")
    Optional<ScrapingRun> findByIdForUpdate(@Param("id") UUID id);
}
