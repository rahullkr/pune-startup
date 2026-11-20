package com.punestartup.job.controller;

import com.punestartup.common.web.PageResponse;
import com.punestartup.job.dto.AdminJobDetailResponse;
import com.punestartup.job.dto.AdminJobSourceResponse;
import com.punestartup.job.dto.AdminJobSummaryResponse;
import com.punestartup.job.dto.CreateJobRequest;
import com.punestartup.job.dto.FlagDuplicateRequest;
import com.punestartup.job.dto.JobSourceInput;
import com.punestartup.job.dto.MergeJobsRequest;
import com.punestartup.job.dto.UpdateJobRequest;
import com.punestartup.job.dto.UpdateJobSourceRequest;
import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.job.service.AdminJobService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/jobs")
public class AdminJobController {

    private final AdminJobService jobService;

    public AdminJobController(AdminJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public PageResponse<AdminJobSummaryResponse> listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false, defaultValue = "updated") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(jobService.listJobs(keyword, companyId, status, employmentType, workMode,
                sort, page, size));
    }

    @GetMapping("/duplicates")
    public List<AdminJobSummaryResponse> findDuplicates(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) String applicationUrl,
            @RequestParam(required = false) UUID excludeId) {
        return jobService.findDuplicates(companyId, title, sourceUrl, applicationUrl, excludeId);
    }

    @GetMapping("/{id}")
    public AdminJobDetailResponse getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    @PostMapping
    public ResponseEntity<AdminJobDetailResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        AdminJobDetailResponse created = jobService.createJob(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/jobs/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public AdminJobDetailResponse updateJob(@PathVariable UUID id,
                                            @Valid @RequestBody UpdateJobRequest request) {
        return jobService.updateJob(id, request);
    }

    @PostMapping("/{id}/publish")
    public AdminJobDetailResponse publishJob(@PathVariable UUID id) {
        return jobService.publish(id);
    }

    @PostMapping("/{id}/expire")
    public AdminJobDetailResponse expireJob(@PathVariable UUID id) {
        return jobService.expire(id);
    }

    @PostMapping("/{id}/remove")
    public AdminJobDetailResponse removeJob(@PathVariable UUID id) {
        return jobService.remove(id);
    }

    @PostMapping("/{id}/mark-stale")
    public AdminJobDetailResponse markJobStale(@PathVariable UUID id) {
        return jobService.markStale(id);
    }

    @PostMapping("/{id}/mark-unverified")
    public AdminJobDetailResponse markJobUnverified(@PathVariable UUID id) {
        return jobService.markUnverified(id);
    }

    @PostMapping("/{id}/sources")
    public ResponseEntity<AdminJobSourceResponse> addSource(
            @PathVariable UUID id,
            @Valid @RequestBody JobSourceInput request) {
        AdminJobSourceResponse created = jobService.addSource(id, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/jobs/" + id + "/sources/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}/sources/{sourceId}")
    public AdminJobSourceResponse updateSource(
            @PathVariable UUID id,
            @PathVariable UUID sourceId,
            @Valid @RequestBody UpdateJobSourceRequest request) {
        return jobService.updateSource(id, sourceId, request);
    }

    @PostMapping("/{id}/flag-duplicate")
    public AdminJobDetailResponse flagDuplicate(@PathVariable UUID id,
                                                @Valid @RequestBody FlagDuplicateRequest request) {
        return jobService.flagDuplicate(id, request);
    }

    @PostMapping("/{id}/merge")
    public AdminJobDetailResponse mergeJobs(@PathVariable UUID id,
                                            @Valid @RequestBody MergeJobsRequest request) {
        return jobService.mergeJobs(id, request);
    }
}
