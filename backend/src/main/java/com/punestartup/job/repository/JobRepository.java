package com.punestartup.job.repository;

import com.punestartup.company.entity.RecordStatus;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    Page<Job> findByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, JobStatus status, Pageable pageable);

    List<Job> findTop20ByCompany_IdAndNormalizedTitleOrderByCreatedAtDesc(UUID companyId, String normalizedTitle);

    @EntityGraph(attributePaths = {"company", "company.industry", "companyLocation", "skills", "skills.skill"})
    @Query("""
            SELECT DISTINCT j
            FROM Job j
            JOIN j.company c
            JOIN j.skills js
            WHERE j.status = :jobStatus
              AND c.recordStatus = :companyStatus
              AND js.skill.id IN :skillIds
            """)
    List<Job> findActiveJobsMatchingAnySkill(@Param("jobStatus") JobStatus jobStatus,
                                             @Param("companyStatus") RecordStatus companyStatus,
                                             @Param("skillIds") Collection<UUID> skillIds);

    @Query(value = """
            SELECT company_id AS companyId, COUNT(*) AS jobCount
            FROM jobs
            WHERE status = 'ACTIVE' AND company_id IN (:companyIds)
            GROUP BY company_id
            """, nativeQuery = true)
    List<CompanyJobCountProjection> countActiveByCompanyIds(@Param("companyIds") Collection<UUID> companyIds);

    @Query(value = """
            SELECT company_id AS companyId, COUNT(*) AS jobCount
            FROM jobs
            WHERE status = 'ACTIVE'
              AND (experience_min_years IS NULL OR experience_min_years <= :fresherMaxYears)
              AND company_id IN (:companyIds)
            GROUP BY company_id
            """, nativeQuery = true)
    List<CompanyJobCountProjection> countFresherEligibleByCompanyIds(@Param("companyIds") Collection<UUID> companyIds,
                                                                     @Param("fresherMaxYears") BigDecimal fresherMaxYears);
}
