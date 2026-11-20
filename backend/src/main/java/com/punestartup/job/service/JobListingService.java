package com.punestartup.job.service;

import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.web.PageRequests;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.job.dto.JobSummaryResponse;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.repository.JobRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobListingService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final JobSummaryMapper summaryMapper;
    private final int maxPageSize;

    public JobListingService(JobRepository jobRepository,
                             CompanyRepository companyRepository,
                             JobSummaryMapper summaryMapper,
                             @Value("${app.search.max-page-size}") int maxPageSize) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.summaryMapper = summaryMapper;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<JobSummaryResponse> listActiveJobs(UUID companyId, int page, int size) {
        companyRepository.findById(companyId)
                .filter(company -> company.getRecordStatus() == RecordStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));

        PageRequest pageRequest = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"), maxPageSize);
        return jobRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, JobStatus.ACTIVE, pageRequest)
                .map(summaryMapper::toSummary);
    }
}
