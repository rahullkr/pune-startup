package com.punestartup.job.controller;

import com.punestartup.common.web.PageResponse;
import com.punestartup.job.dto.JobDetailResponse;
import com.punestartup.job.dto.JobSearchRequest;
import com.punestartup.job.dto.JobSummaryResponse;
import com.punestartup.job.service.JobQueryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class JobController {

    private final JobQueryService jobQueryService;

    public JobController(JobQueryService jobQueryService) {
        this.jobQueryService = jobQueryService;
    }

    @GetMapping("/jobs")
    public PageResponse<JobSummaryResponse> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) Boolean fresher,
            @RequestParam(required = false) BigDecimal experienceYears,
            @RequestParam(required = false) List<String> skill,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        JobSearchRequest request = JobSearchRequest.of(keyword, companyId, city, area, industry, employmentType,
                workMode, fresher, experienceYears, skill, page, size, sort);
        return PageResponse.from(jobQueryService.searchJobs(request));
    }

    @GetMapping("/jobs/{id}")
    public JobDetailResponse getJob(@PathVariable UUID id) {
        return jobQueryService.getJobDetail(id);
    }
}
