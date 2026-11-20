package com.punestartup.ingestion.service;

import com.punestartup.ingestion.entity.ScrapingRun;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobSource;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.JobStatusHistory;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSourceRepository;
import com.punestartup.job.repository.JobStatusHistoryRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobLifecycleService {

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobStatusHistoryRepository statusHistoryRepository;
    private final int staleAfterMissingChecks;
    private final int expireAfterMissingChecks;

    public JobLifecycleService(JobRepository jobRepository,
                               JobSourceRepository jobSourceRepository,
                               JobStatusHistoryRepository statusHistoryRepository,
                               @Value("${app.ingestion.freshness.stale-after-missing-checks:2}")
                               int staleAfterMissingChecks,
                               @Value("${app.ingestion.freshness.expire-after-missing-checks:6}")
                               int expireAfterMissingChecks) {
        if (staleAfterMissingChecks < 1) {
            throw new IllegalArgumentException("stale-after-missing-checks must be at least 1");
        }
        if (expireAfterMissingChecks < staleAfterMissingChecks) {
            throw new IllegalArgumentException(
                    "expire-after-missing-checks must not be lower than stale-after-missing-checks");
        }
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.staleAfterMissingChecks = staleAfterMissingChecks;
        this.expireAfterMissingChecks = expireAfterMissingChecks;
    }

    @Transactional
    public void onJobConfirmed(Job job, JobSource source, ScrapingRun run, Instant now) {
        if (job.getFirstSeenAt() == null) {
            job.setFirstSeenAt(now);
        }
        job.setLastSeenAt(now);
        job.setLastCheckedAt(now);
        job.setLastVerifiedAt(now);
        if (job.getStatus() == JobStatus.STALE) {
            transition(job, JobStatus.ACTIVE,
                    "Source '" + source.getSourceName() + "' confirms the listing again",
                    run.getId(), now);
            job.setStaleSince(null);
        }
        jobRepository.save(job);
    }

    @Transactional
    public void reconcileCompletedRun(ScrapingRun run) {
        Instant now = Instant.now();
        if (run.getFinishedAt() == null
                || run.getStatus() != ScrapingRunStatus.SUCCEEDED
                || run.getFreshnessReconciledAt() != null
                || run.getRecordsProcessed() < run.getRecordsEmitted()) {
            return;
        }

        ScrapingSource scrapingSource = run.getSource();
        List<JobSource> sources = jobSourceRepository.findForScrapingSource(
                scrapingSource.getId(), scrapingSource.getSourceName());
        Set<UUID> affectedJobs = new LinkedHashSet<>();
        for (JobSource source : sources) {
            Instant lastChecked = source.getLastCheckedAt();
            boolean seenInRun = lastChecked != null && !lastChecked.isBefore(run.getStartedAt());
            if (seenInRun) {
                continue;
            }
            markMissing(source, run, now);
            affectedJobs.add(source.getJob().getId());
        }
        for (UUID jobId : affectedJobs) {
            evaluateJob(jobId, scrapingSource, run, now);
        }
        run.setFreshnessReconciledAt(now);
    }

    @Transactional
    public void transition(Job job, JobStatus newStatus, String reason, UUID scrapingRunId, Instant now) {
        JobStatus oldStatus = job.getStatus();
        if (oldStatus == newStatus) {
            return;
        }
        job.setStatus(newStatus);
        JobStatusHistory history = new JobStatusHistory();
        history.setId(UUID.randomUUID());
        history.setJob(job);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setScrapingRunId(scrapingRunId);
        history.setReason(reason);
        history.setChangedAt(now);
        statusHistoryRepository.save(history);
    }

    private void markMissing(JobSource source, ScrapingRun run, Instant now) {
        int missingChecks = source.getConsecutiveMissingChecks() + 1;
        source.setConsecutiveMissingChecks(missingChecks);
        source.setLastCheckedAt(now);
        source.setLastMissingAt(now);
        source.setLastMissingRunId(run.getId());
        if (missingChecks >= staleAfterMissingChecks) {
            source.setSourceStatus("REMOVED");
            if (source.getRemovedAt() == null) {
                source.setRemovedAt(now);
            }
        } else {
            source.setSourceStatus("MISSING");
        }
        jobSourceRepository.save(source);
    }

    private void evaluateJob(UUID jobId, ScrapingSource scrapingSource, ScrapingRun run, Instant now) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        List<JobSource> allSources = jobSourceRepository.findByJob_IdOrderByFirstSeenAtAsc(jobId);
        boolean anyActiveSource = allSources.stream()
                .anyMatch(source -> "ACTIVE".equals(source.getSourceStatus()));
        int maxMissingChecks = allSources.stream()
                .mapToInt(JobSource::getConsecutiveMissingChecks)
                .max()
                .orElse(0);

        job.setLastCheckedAt(now);
        if (job.getFirstSeenAt() == null) {
            job.setFirstSeenAt(now);
        }
        if (job.getLastSeenAt() == null) {
            job.setLastSeenAt(job.getFirstSeenAt());
        }

        if (job.getStatus() == JobStatus.ACTIVE
                && !anyActiveSource
                && maxMissingChecks >= staleAfterMissingChecks) {
            transition(job, JobStatus.STALE,
                    "No source confirms the listing after " + maxMissingChecks
                            + " consecutive missing checks (source '" + scrapingSource.getSourceName() + "')",
                    run.getId(), now);
            job.setStaleSince(now);
        } else if (job.getStatus() == JobStatus.STALE) {
            if (anyActiveSource) {
                transition(job, JobStatus.ACTIVE,
                        "A source confirms the listing again (source '" + scrapingSource.getSourceName() + "')",
                        run.getId(), now);
                job.setStaleSince(null);
                job.setLastSeenAt(now);
            } else if (maxMissingChecks >= expireAfterMissingChecks) {
                transition(job, JobStatus.EXPIRED,
                        "Removed after " + maxMissingChecks + " consecutive missing checks",
                        run.getId(), now);
                job.setExpiredAt(now);
            }
        }

        if (job.getExpiresAt() != null
                && !job.getExpiresAt().isAfter(now)
                && (job.getStatus() == JobStatus.ACTIVE || job.getStatus() == JobStatus.STALE)) {
            transition(job, JobStatus.EXPIRED, "Listing expiry date reached", run.getId(), now);
            job.setExpiredAt(now);
        }
        jobRepository.save(job);
    }
}
