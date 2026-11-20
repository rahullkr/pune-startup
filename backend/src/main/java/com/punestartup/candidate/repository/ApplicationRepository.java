package com.punestartup.candidate.repository;

import com.punestartup.candidate.entity.Application;
import com.punestartup.candidate.entity.ApplicationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @EntityGraph(attributePaths = {"job", "job.company", "job.company.industry", "job.companyLocation"})
    Page<Application> findByCandidateProfile_Id(UUID candidateProfileId, Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company", "job.company.industry", "job.companyLocation"})
    Page<Application> findByCandidateProfile_IdAndStatus(UUID candidateProfileId, ApplicationStatus status,
                                                         Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.company", "job.company.industry", "job.companyLocation"})
    Optional<Application> findByIdAndCandidateProfile_Id(UUID id, UUID candidateProfileId);

    boolean existsByCandidateProfile_IdAndJob_Id(UUID candidateProfileId, UUID jobId);

    long countByCandidateProfile_Id(UUID candidateProfileId);
}
