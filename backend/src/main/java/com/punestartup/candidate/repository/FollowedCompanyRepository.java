package com.punestartup.candidate.repository;

import com.punestartup.candidate.entity.FollowedCompany;
import com.punestartup.candidate.entity.FollowedCompanyId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowedCompanyRepository extends JpaRepository<FollowedCompany, FollowedCompanyId> {

    @EntityGraph(attributePaths = {"company", "company.industry"})
    Page<FollowedCompany> findByCandidateProfile_Id(UUID candidateProfileId, Pageable pageable);

    Optional<FollowedCompany> findByCandidateProfile_IdAndCompany_Id(UUID candidateProfileId, UUID companyId);

    boolean existsByCandidateProfile_IdAndCompany_Id(UUID candidateProfileId, UUID companyId);

    long countByCandidateProfile_Id(UUID candidateProfileId);
}
