package com.punestartup.company.controller;

import com.punestartup.common.web.PageResponse;
import com.punestartup.company.dto.CompanyDetailResponse;
import com.punestartup.company.dto.CompanySearchRequest;
import com.punestartup.company.dto.CompanySummaryResponse;
import com.punestartup.company.service.CompanyQueryService;
import com.punestartup.job.dto.JobSummaryResponse;
import com.punestartup.job.service.JobListingService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CompanyController {

    private final CompanyQueryService companyQueryService;
    private final JobListingService jobListingService;

    public CompanyController(CompanyQueryService companyQueryService, JobListingService jobListingService) {
        this.companyQueryService = companyQueryService;
        this.jobListingService = jobListingService;
    }

    @GetMapping("/companies")
    public PageResponse<CompanySummaryResponse> searchCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false, defaultValue = "ALL") String roleType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        CompanySearchRequest request = CompanySearchRequest.of(keyword, city, area, industry, roleType, page, size,
                sort);
        Page<CompanySummaryResponse> result = companyQueryService.searchCompanies(request);
        return PageResponse.from(result);
    }

    @GetMapping("/companies/areas")
    public List<String> listCompanyAreas() {
        return companyQueryService.listAreas();
    }

    @GetMapping("/companies/{id}")
    public CompanyDetailResponse getCompany(@PathVariable UUID id) {
        return companyQueryService.getCompanyDetail(id);
    }

    @GetMapping("/companies/{id}/jobs")
    public PageResponse<JobSummaryResponse> getCompanyJobs(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(jobListingService.listActiveJobs(id, page, size));
    }
}
