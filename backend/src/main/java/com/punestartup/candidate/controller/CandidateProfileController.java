package com.punestartup.candidate.controller;

import com.punestartup.auth.security.CandidatePrincipal;
import com.punestartup.candidate.dto.CandidateProfileResponse;
import com.punestartup.candidate.dto.UpdateCandidateProfileRequest;
import com.punestartup.candidate.service.CandidateProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidates/me")
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;

    public CandidateProfileController(CandidateProfileService candidateProfileService) {
        this.candidateProfileService = candidateProfileService;
    }

    @GetMapping
    public CandidateProfileResponse getProfile(@AuthenticationPrincipal CandidatePrincipal principal) {
        return candidateProfileService.getProfile(principal.userId());
    }

    @PatchMapping
    public CandidateProfileResponse updateProfile(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @Valid @RequestBody UpdateCandidateProfileRequest request) {
        return candidateProfileService.updateProfile(principal.userId(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal CandidatePrincipal principal) {
        candidateProfileService.deactivate(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
