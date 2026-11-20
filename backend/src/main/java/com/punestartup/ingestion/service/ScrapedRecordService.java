package com.punestartup.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.web.PageRequests;
import com.punestartup.ingestion.dto.ScrapedRecordDetailResponse;
import com.punestartup.ingestion.dto.ScrapedRecordSummaryResponse;
import com.punestartup.ingestion.entity.ScrapedRecord;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.repository.ScrapedRecordRepository;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.repository.JobRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScrapedRecordService {

    private final ScrapedRecordRepository recordRepository;
    private final JobRepository jobRepository;
    private final JobLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;
    private final int maxPageSize;

    public ScrapedRecordService(ScrapedRecordRepository recordRepository,
                                JobRepository jobRepository,
                                JobLifecycleService lifecycleService,
                                ObjectMapper objectMapper,
                                @Value("${app.search.max-page-size}") int maxPageSize) {
        this.recordRepository = recordRepository;
        this.jobRepository = jobRepository;
        this.lifecycleService = lifecycleService;
        this.objectMapper = objectMapper;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<ScrapedRecordSummaryResponse> listRecords(UUID runId, UUID sourceId,
                                                          ScrapedRecordStatus status,
                                                          int page, int size) {
        var pageRequest = PageRequests.of(page, size, Sort.by(Sort.Order.desc("createdAt")), maxPageSize);
        return recordRepository.findAll(toSpecification(runId, sourceId, status), pageRequest)
                .map(ScrapedRecordSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ScrapedRecordDetailResponse getRecord(UUID id) {
        return ScrapedRecordDetailResponse.from(findRecord(id), objectMapper);
    }

    @Transactional
    public ScrapedRecordDetailResponse publish(UUID id) {
        ScrapedRecord record = findRecord(id);
        Job job = requireReviewableJob(record);
        if (job.getStatus() == JobStatus.NEEDS_REVIEW) {
            Instant now = Instant.now();
            lifecycleService.transition(job, JobStatus.ACTIVE, "Published by administrator",
                    record.getRun().getId(), now);
            job.setLastVerifiedAt(now);
            jobRepository.save(job);
        } else if (job.getStatus() != JobStatus.ACTIVE) {
            throw InvalidParameterException.of("jobStatus", "only NEEDS_REVIEW jobs can be published");
        }
        record.setProcessingStatus(ScrapedRecordStatus.PROCESSED);
        record.setProcessedAt(Instant.now());
        recordRepository.save(record);
        return ScrapedRecordDetailResponse.from(record, objectMapper);
    }

    @Transactional
    public ScrapedRecordDetailResponse reject(UUID id) {
        ScrapedRecord record = findRecord(id);
        Job job = requireReviewableJob(record);
        if (job.getStatus() == JobStatus.NEEDS_REVIEW || job.getStatus() == JobStatus.ACTIVE) {
            lifecycleService.transition(job, JobStatus.REJECTED, "Rejected by administrator",
                    record.getRun().getId(), Instant.now());
            jobRepository.save(job);
        } else if (job.getStatus() != JobStatus.REJECTED) {
            throw InvalidParameterException.of("jobStatus", "job cannot be rejected in its current state");
        }
        record.setProcessingStatus(ScrapedRecordStatus.SKIPPED);
        if (record.getErrorMessage() == null) {
            record.setErrorMessage("Rejected by admin");
        }
        record.setProcessedAt(Instant.now());
        recordRepository.save(record);
        return ScrapedRecordDetailResponse.from(record, objectMapper);
    }

    private Job requireReviewableJob(ScrapedRecord record) {
        if (record.getJob() == null) {
            throw InvalidParameterException.of("jobId", "record has no canonical job to review");
        }
        return record.getJob();
    }

    private ScrapedRecord findRecord(UUID id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scraped record", id));
    }

    private Specification<ScrapedRecord> toSpecification(UUID runId, UUID sourceId, ScrapedRecordStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (runId != null) {
                predicates.add(cb.equal(root.get("run").get("id"), runId));
            }
            if (sourceId != null) {
                predicates.add(cb.equal(root.get("run").get("source").get("id"), sourceId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("processingStatus"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
