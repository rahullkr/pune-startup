package com.punestartup.resume.service;

import com.punestartup.common.util.TextNormalizer;
import com.punestartup.skill.entity.Skill;
import com.punestartup.skill.repository.SkillRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ResumeSkillMatcher {

    private final SkillRepository skillRepository;

    public ResumeSkillMatcher(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<Skill> match(String text) {
        String normalizedText = TextNormalizer.normalizeName(text);
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }
        List<Skill> matched = new ArrayList<>();
        for (Skill skill : skillRepository.findAll()) {
            String normalizedName = skill.getNormalizedName();
            if (normalizedName == null || normalizedName.isBlank()) {
                continue;
            }
            Pattern pattern = Pattern.compile(
                    "(?<![a-z0-9])" + Pattern.quote(normalizedName) + "(?![a-z0-9])");
            if (pattern.matcher(normalizedText).find()) {
                matched.add(skill);
            }
        }
        matched.sort(Comparator.comparing(Skill::getName, String.CASE_INSENSITIVE_ORDER));
        return matched;
    }
}
