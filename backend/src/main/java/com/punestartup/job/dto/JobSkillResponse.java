package com.punestartup.job.dto;

import com.punestartup.job.entity.JobSkill;
import java.util.UUID;

public record JobSkillResponse(
        UUID skillId,
        String skillName,
        String category,
        String requirementType) {

    public static JobSkillResponse from(JobSkill jobSkill) {
        return new JobSkillResponse(
                jobSkill.getSkill().getId(),
                jobSkill.getSkill().getName(),
                jobSkill.getSkill().getCategory(),
                jobSkill.getRequirementType() == null ? null : jobSkill.getRequirementType().name());
    }
}
