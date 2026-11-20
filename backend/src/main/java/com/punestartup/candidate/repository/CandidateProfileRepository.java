package com.punestartup.candidate.repository;

import com.punestartup.candidate.entity.CandidateProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

    Optional<CandidateProfile> findByUser_Id(UUID userId);
}
