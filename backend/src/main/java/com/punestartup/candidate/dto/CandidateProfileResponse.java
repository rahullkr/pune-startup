package com.punestartup.candidate.dto;

import com.punestartup.candidate.entity.CandidateProfile;
import com.punestartup.candidate.entity.CandidateSkill;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateProfileResponse(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        String headline,
        String summary,
        Integer totalExperienceMonths,
        String preferredCity,
        String preferredWorkMode,
        List<CandidateSkillResponse> skills,
        Instant createdAt,
        Instant updatedAt) {

    public static CandidateProfileResponse from(CandidateProfile profile, List<CandidateSkill> skills) {
        return new CandidateProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getEmail(),
                profile.getFullName(),
                profile.getHeadline(),
                profile.getSummary(),
                profile.getTotalExperienceMonths(),
                profile.getPreferredCity(),
                profile.getPreferredWorkMode() == null ? null : profile.getPreferredWorkMode().name(),
                skills.stream().map(CandidateSkillResponse::from).toList(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
