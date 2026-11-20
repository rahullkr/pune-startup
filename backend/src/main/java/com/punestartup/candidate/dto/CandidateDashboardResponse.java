package com.punestartup.candidate.dto;

import java.util.List;

public record CandidateDashboardResponse(
        long savedJobCount,
        long followedCompanyCount,
        long applicationCount,
        List<SavedJobResponse> recentSavedJobs,
        List<FollowedCompanyResponse> recentFollowedCompanies,
        List<ApplicationSummaryResponse> recentApplications) {
}
