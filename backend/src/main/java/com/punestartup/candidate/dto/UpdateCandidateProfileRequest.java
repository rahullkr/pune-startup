package com.punestartup.candidate.dto;

import com.punestartup.job.entity.WorkMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateCandidateProfileRequest(
        @Size(min = 1, max = 200) String fullName,
        @Size(max = 250) String headline,
        @Size(max = 20000) String summary,
        @Min(0) @Max(1200) Integer totalExperienceMonths,
        @Size(max = 100) String preferredCity,
        WorkMode preferredWorkMode,
        Boolean clearExperience,
        List<@Valid CandidateSkillInput> skills) {
}
