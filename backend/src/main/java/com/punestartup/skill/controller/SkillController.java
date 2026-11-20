package com.punestartup.skill.controller;

import com.punestartup.skill.dto.SkillResponse;
import com.punestartup.skill.service.SkillService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/skills")
    public List<SkillResponse> listSkills() {
        return skillService.listSkillsUsedByActiveJobs();
    }
}
