package com.punestartup.candidate.controller;

import com.punestartup.auth.security.CandidatePrincipal;
import com.punestartup.candidate.dto.CandidateDashboardResponse;
import com.punestartup.candidate.dto.FollowedCompanyResponse;
import com.punestartup.candidate.dto.SavedJobResponse;
import com.punestartup.candidate.service.CandidateEngagementService;
import com.punestartup.candidate.service.CandidateEngagementService.FollowCompanyResult;
import com.punestartup.candidate.service.CandidateEngagementService.SaveJobResult;
import com.punestartup.common.web.PageResponse;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidates/me")
public class CandidateEngagementController {

    private final CandidateEngagementService engagementService;

    public CandidateEngagementController(CandidateEngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @GetMapping("/saved-jobs")
    public PageResponse<SavedJobResponse> listSavedJobs(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(engagementService.listSavedJobs(principal.userId(), page, size));
    }

    @GetMapping("/saved-jobs/{jobId}")
    public ResponseEntity<SavedJobResponse> getSavedJob(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID jobId) {
        return ResponseEntity.of(engagementService.findSavedJob(principal.userId(), jobId));
    }

    @PutMapping("/saved-jobs/{jobId}")
    public ResponseEntity<SavedJobResponse> saveJob(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID jobId) {
        SaveJobResult result = engagementService.saveJob(principal.userId(), jobId);
        return ResponseEntity.status(result.created() ? 201 : 200)
                .location(URI.create("/api/v1/candidates/me/saved-jobs/" + jobId))
                .body(result.savedJob());
    }

    @DeleteMapping("/saved-jobs/{jobId}")
    public ResponseEntity<Void> unsaveJob(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID jobId) {
        engagementService.unsaveJob(principal.userId(), jobId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/followed-companies")
    public PageResponse<FollowedCompanyResponse> listFollowedCompanies(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(engagementService.listFollowedCompanies(principal.userId(), page, size));
    }

    @GetMapping("/followed-companies/{companyId}")
    public ResponseEntity<FollowedCompanyResponse> getFollowedCompany(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID companyId) {
        return ResponseEntity.of(engagementService.findFollowedCompany(principal.userId(), companyId));
    }

    @PutMapping("/followed-companies/{companyId}")
    public ResponseEntity<FollowedCompanyResponse> followCompany(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID companyId) {
        FollowCompanyResult result = engagementService.followCompany(principal.userId(), companyId);
        return ResponseEntity.status(result.created() ? 201 : 200)
                .location(URI.create("/api/v1/candidates/me/followed-companies/" + companyId))
                .body(result.company());
    }

    @DeleteMapping("/followed-companies/{companyId}")
    public ResponseEntity<Void> unfollowCompany(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID companyId) {
        engagementService.unfollowCompany(principal.userId(), companyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public CandidateDashboardResponse getDashboard(@AuthenticationPrincipal CandidatePrincipal principal) {
        return engagementService.getDashboard(principal.userId());
    }
}
