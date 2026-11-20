package com.punestartup.skill.service;

import com.punestartup.skill.dto.SkillResponse;
import com.punestartup.skill.repository.SkillRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkillsUsedByActiveJobs() {
        return skillRepository.findSkillsUsedByActiveJobs().stream()
                .map(SkillResponse::from)
                .sorted(Comparator.comparing(SkillResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
