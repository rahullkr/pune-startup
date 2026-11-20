package com.punestartup.resume.repository;

import com.punestartup.resume.entity.CandidateResume;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateResumeRepository extends JpaRepository<CandidateResume, UUID> {

    @EntityGraph(attributePaths = {"candidateProfile"})
    Optional<CandidateResume> findByIdAndCandidateProfile_User_IdAndDeletedAtIsNull(
            UUID id, UUID userId);

    @EntityGraph(attributePaths = {"candidateProfile"})
    List<CandidateResume> findByCandidateProfile_IdAndDeletedAtIsNullAndRetentionExpiresAtAfterOrderByUploadedAtDesc(
            UUID candidateProfileId, Instant now);

    @EntityGraph(attributePaths = {"candidateProfile"})
    List<CandidateResume> findByCandidateProfile_IdAndDeletedAtIsNull(UUID candidateProfileId);

    List<CandidateResume> findTop100ByDeletedAtIsNullAndRetentionExpiresAtLessThanEqualOrderByRetentionExpiresAtAsc(
            Instant now);

    List<CandidateResume> findByCandidateProfile_IdAndDeletedAtIsNullAndPrimaryTrue(UUID candidateProfileId);
}
