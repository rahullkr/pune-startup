package com.punestartup.job.repository;

import com.punestartup.job.entity.JobSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobSourceRepository extends JpaRepository<JobSource, UUID> {

    List<JobSource> findByJob_IdOrderByFirstSeenAtAsc(UUID jobId);

    Optional<JobSource> findFirstBySourceNameAndSourceJobId(String sourceName, String sourceJobId);

    @Query("""
            select js
            from JobSource js
            where js.applicationUrl = :url or js.sourceUrl = :url
            order by js.firstSeenAt asc
            """)
    List<JobSource> findByCanonicalUrl(@Param("url") String url);

    Optional<JobSource> findFirstByJob_Company_IdAndJob_NormalizedTitleAndJob_CompanyLocation_IdOrderByFirstSeenAtAsc(
            UUID companyId, String normalizedTitle, UUID locationId);

    Optional<JobSource> findFirstByJob_Company_IdAndJob_NormalizedTitleAndJob_CompanyLocationIsNullOrderByFirstSeenAtAsc(
            UUID companyId, String normalizedTitle);

    @Query("""
            select js
            from JobSource js
            where js.scrapingSourceId = :sourceId
               or (js.scrapingSourceId is null and js.sourceName = :sourceName)
            """)
    List<JobSource> findForScrapingSource(@Param("sourceId") UUID sourceId,
                                          @Param("sourceName") String sourceName);

    boolean existsByJob_IdAndSourceStatus(UUID jobId, String sourceStatus);
}
