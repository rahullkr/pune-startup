package com.punestartup.candidate.controller;

import com.punestartup.auth.security.CandidatePrincipal;
import com.punestartup.candidate.dto.ApplicationDetailResponse;
import com.punestartup.candidate.dto.ApplicationSummaryResponse;
import com.punestartup.candidate.dto.CreateApplicationRequest;
import com.punestartup.candidate.dto.UpdateApplicationRequest;
import com.punestartup.candidate.entity.ApplicationStatus;
import com.punestartup.candidate.service.CandidateApplicationService;
import com.punestartup.common.web.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidates/me/applications")
public class CandidateApplicationController {

    private final CandidateApplicationService applicationService;

    public CandidateApplicationController(CandidateApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public PageResponse<ApplicationSummaryResponse> listApplications(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(applicationService.listApplications(principal.userId(), status, page, size));
    }

    @GetMapping("/{applicationId}")
    public ApplicationDetailResponse getApplication(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID applicationId) {
        return applicationService.getApplication(principal.userId(), applicationId);
    }

    @PostMapping
    public ResponseEntity<ApplicationDetailResponse> createApplication(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationDetailResponse created = applicationService.createApplication(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/candidates/me/applications/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{applicationId}")
    public ApplicationDetailResponse updateApplication(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationRequest request) {
        return applicationService.updateApplication(principal.userId(), applicationId, request);
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> deleteApplication(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @PathVariable UUID applicationId) {
        applicationService.deleteApplication(principal.userId(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
