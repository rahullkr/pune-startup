package com.punestartup.skill.dto;

import com.punestartup.skill.entity.Skill;
import java.util.UUID;

public record SkillResponse(
        UUID id,
        String name,
        String category) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory());
    }
}
