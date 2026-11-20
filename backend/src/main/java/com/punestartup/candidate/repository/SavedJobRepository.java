package com.punestartup.candidate.repository;

import com.punestartup.candidate.entity.SavedJob;
import com.punestartup.candidate.entity.SavedJobId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedJobRepository extends JpaRepository<SavedJob, SavedJobId> {

    @EntityGraph(attributePaths = {"job", "job.company", "job.company.industry", "job.companyLocation"})
    Page<SavedJob> findByCandidateProfile_Id(UUID candidateProfileId, Pageable pageable);

    Optional<SavedJob> findByCandidateProfile_IdAndJob_Id(UUID candidateProfileId, UUID jobId);

    boolean existsByCandidateProfile_IdAndJob_Id(UUID candidateProfileId, UUID jobId);

    long countByCandidateProfile_Id(UUID candidateProfileId);
}
