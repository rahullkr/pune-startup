package com.punestartup.candidate.dto;

import com.punestartup.job.dto.JobSummaryResponse;
import java.util.List;

public record JobMatchResponse(
        int matchScore,
        int matchedSkillCount,
        int requiredSkillCount,
        int matchedRequiredSkillCount,
        int preferredSkillCount,
        int matchedPreferredSkillCount,
        JobSummaryResponse job,
        List<MatchedSkillResponse> matchedSkills,
        List<MissingSkillResponse> missingSkills,
        List<MatchSignalResponse> signals) {
}
