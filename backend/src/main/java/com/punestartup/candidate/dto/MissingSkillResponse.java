package com.punestartup.candidate.dto;

import com.punestartup.job.entity.JobSkill;
import java.util.UUID;

public record MissingSkillResponse(
        UUID skillId,
        String skillName,
        String requirementType) {

    public static MissingSkillResponse from(JobSkill jobSkill) {
        return new MissingSkillResponse(
                jobSkill.getSkill().getId(),
                jobSkill.getSkill().getName(),
                jobSkill.getRequirementType() == null ? null : jobSkill.getRequirementType().name());
    }
}
