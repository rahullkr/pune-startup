package com.punestartup.candidate.controller;

import com.punestartup.auth.security.CandidatePrincipal;
import com.punestartup.candidate.dto.JobMatchResponse;
import com.punestartup.candidate.service.CandidateJobMatchService;
import com.punestartup.common.web.PageResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidates/me")
public class CandidateJobMatchController {

    private final CandidateJobMatchService jobMatchService;

    public CandidateJobMatchController(CandidateJobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    @GetMapping("/job-matches")
    public PageResponse<JobMatchResponse> listJobMatches(
            @AuthenticationPrincipal CandidatePrincipal principal,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(jobMatchService.matchJobs(principal.userId(), page, size));
    }
}
