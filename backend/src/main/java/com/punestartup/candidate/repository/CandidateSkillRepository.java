package com.punestartup.candidate.repository;

import com.punestartup.candidate.entity.CandidateSkill;
import com.punestartup.candidate.entity.CandidateSkillId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, CandidateSkillId> {

    @EntityGraph(attributePaths = {"skill"})
    List<CandidateSkill> findByCandidateProfile_Id(UUID candidateProfileId);

    List<CandidateSkill> findByCandidateProfile_IdOrderBySkill_NameAsc(UUID candidateProfileId);
}
