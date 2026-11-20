package com.punestartup.resume.controller;

import com.punestartup.auth.security.CandidatePrincipal;
import com.punestartup.resume.dto.ResumeResponse;
import com.punestartup.resume.service.ResumeService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/candidates/me/resumes")
public class CandidateResumeController {

    private final ResumeService resumeService;

    public CandidateResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> upload(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "consent", required = false) Boolean consent,
            @RequestParam(value = "consentVersion", defaultValue = "v1") String consentVersion,
            @RequestParam(value = "primary", defaultValue = "true") boolean primary) {
        ResumeResponse response = resumeService.upload(
                principal.userId(), file, Boolean.TRUE.equals(consent), consentVersion, primary);
        return ResponseEntity.created(
                        URI.create("/api/v1/candidates/me/resumes/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<ResumeResponse> list(@AuthenticationPrincipal CandidatePrincipal principal) {
        return resumeService.list(principal.userId());
    }

    @GetMapping("/{resumeId}")
    public ResumeResponse get(@AuthenticationPrincipal CandidatePrincipal principal,
                              @PathVariable UUID resumeId) {
        return resumeService.get(principal.userId(), resumeId);
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CandidatePrincipal principal,
                                       @PathVariable UUID resumeId) {
        resumeService.delete(principal.userId(), resumeId);
        return ResponseEntity.noContent().build();
    }
}
