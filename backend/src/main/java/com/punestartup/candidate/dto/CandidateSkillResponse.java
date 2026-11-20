package com.punestartup.candidate.dto;

import com.punestartup.candidate.entity.CandidateSkill;
import java.math.BigDecimal;
import java.util.UUID;

public record CandidateSkillResponse(
        UUID skillId,
        String skillName,
        String category,
        String proficiency,
        BigDecimal yearsExperience,
        String source) {

    public static CandidateSkillResponse from(CandidateSkill candidateSkill) {
        return new CandidateSkillResponse(
                candidateSkill.getSkill().getId(),
                candidateSkill.getSkill().getName(),
                candidateSkill.getSkill().getCategory(),
                candidateSkill.getProficiency(),
                candidateSkill.getYearsExperience(),
                candidateSkill.getSource().name());
    }
}
