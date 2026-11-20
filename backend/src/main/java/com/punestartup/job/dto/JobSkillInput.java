package com.punestartup.job.dto;

import com.punestartup.job.entity.SkillRequirementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobSkillInput(
        @NotBlank @Size(max = 120) String name,
        SkillRequirementType requirementType) {
}
