package com.punestartup.ingestion.repository;

import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.ingestion.entity.PermissionReviewStatus;
import java.util.UUID;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScrapingSourceRepository
        extends JpaRepository<ScrapingSource, UUID>, JpaSpecificationExecutor<ScrapingSource> {

    List<ScrapingSource> findByPermissionReviewStatusAndEnabledTrue(PermissionReviewStatus permissionReviewStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScrapingSource s where s.id = :id")
    java.util.Optional<ScrapingSource> findByIdForUpdate(@Param("id") UUID id);
}
