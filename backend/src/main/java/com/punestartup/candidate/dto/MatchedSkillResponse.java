package com.punestartup.candidate.dto;

import com.punestartup.candidate.entity.CandidateSkill;
import com.punestartup.job.entity.JobSkill;
import java.math.BigDecimal;
import java.util.UUID;

public record MatchedSkillResponse(
        UUID skillId,
        String skillName,
        String requirementType,
        String proficiency,
        BigDecimal yearsExperience) {

    public static MatchedSkillResponse from(JobSkill jobSkill, CandidateSkill candidateSkill) {
        return new MatchedSkillResponse(
                jobSkill.getSkill().getId(),
                jobSkill.getSkill().getName(),
                jobSkill.getRequirementType() == null ? null : jobSkill.getRequirementType().name(),
                candidateSkill.getProficiency(),
                candidateSkill.getYearsExperience());
    }
}
