package com.punestartup.ingestion.service;

import com.punestartup.common.exception.ConflictException;
import com.punestartup.common.exception.DependencyUnavailableException;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.RateLimitException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.web.PageRequests;
import com.punestartup.ingestion.dto.ScrapingRunResponse;
import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ProcessedIngestionEvent;
import com.punestartup.ingestion.entity.ScrapingRun;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import com.punestartup.ingestion.entity.ScrapingRunTrigger;
import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.ingestion.event.ScrapeRunCompletedEvent;
import com.punestartup.ingestion.event.ScrapeSourcePayload;
import com.punestartup.ingestion.event.ScrapeSourceRequestedCommand;
import com.punestartup.ingestion.messaging.IngestionCommandPublisher;
import com.punestartup.ingestion.repository.ProcessedIngestionEventRepository;
import com.punestartup.ingestion.repository.ScrapingRunRepository;
import com.punestartup.ingestion.repository.ScrapingSourceRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScrapingRunService {

    private static final Logger log = LoggerFactory.getLogger(ScrapingRunService.class);

    private final ScrapingSourceRepository sourceRepository;
    private final ScrapingRunRepository runRepository;
    private final ProcessedIngestionEventRepository processedEventRepository;
    private final IngestionCommandPublisher commandPublisher;
    private final JobLifecycleService lifecycleService;
    private final int maxPageSize;
    private final boolean schedulingEnabled;
    private final Duration missedRunGrace;

    private static final EnumSet<ScrapingRunStatus> ACTIVE_ATTEMPT_STATUSES = EnumSet.of(
            ScrapingRunStatus.RUNNING, ScrapingRunStatus.SUCCEEDED,
            ScrapingRunStatus.PARTIAL, ScrapingRunStatus.FAILED);

    public ScrapingRunService(ScrapingSourceRepository sourceRepository,
                              ScrapingRunRepository runRepository,
                              ProcessedIngestionEventRepository processedEventRepository,
                              IngestionCommandPublisher commandPublisher,
                              JobLifecycleService lifecycleService,
                              @Value("${app.search.max-page-size}") int maxPageSize,
                              @Value("${app.ingestion.scheduler.enabled:true}") boolean schedulingEnabled,
                              @Value("${app.ingestion.scheduler.missed-run-grace:PT15M}") Duration missedRunGrace) {
        this.sourceRepository = sourceRepository;
        this.runRepository = runRepository;
        this.processedEventRepository = processedEventRepository;
        this.commandPublisher = commandPublisher;
        this.lifecycleService = lifecycleService;
        this.maxPageSize = maxPageSize;
        this.schedulingEnabled = schedulingEnabled;
        this.missedRunGrace = missedRunGrace;
    }

    @Transactional(readOnly = true)
    public Page<ScrapingRunResponse> listRuns(UUID sourceId, ScrapingRunStatus status, int page, int size) {
        var pageRequest = PageRequests.of(page, size, Sort.by(Sort.Order.desc("startedAt")), maxPageSize);
        return runRepository.findAll(toSpecification(sourceId, status), pageRequest).map(ScrapingRunResponse::from);
    }

    @Transactional(readOnly = true)
    public ScrapingRunResponse getRun(UUID id) {
        return ScrapingRunResponse.from(findRun(id));
    }

    @Transactional
    public ScrapingRunResponse startRun(UUID sourceId) {
        ScrapingSource source = sourceRepository.findByIdForUpdate(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping source", sourceId));
        return startRun(source, null, ScrapingRunTrigger.MANUAL);
    }

    @Transactional
    public ScrapingRunResponse retryRun(UUID runId) {
        ScrapingRun original = findRun(runId);
        if (original.getFinishedAt() == null || original.getStatus() == ScrapingRunStatus.RUNNING) {
            throw InvalidParameterException.of("status", "a run in progress cannot be retried");
        }
        if (original.getStatus() != ScrapingRunStatus.FAILED && original.getStatus() != ScrapingRunStatus.PARTIAL) {
            throw InvalidParameterException.of("status", "only FAILED or PARTIAL runs can be retried");
        }
        ScrapingSource source = sourceRepository.findByIdForUpdate(original.getSource().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Scraping source", original.getSource().getId()));
        return startRun(source, original.getId(), ScrapingRunTrigger.RETRY);
    }

    @Scheduled(fixedDelayString = "${app.ingestion.scheduler.poll-interval:PT1M}",
            initialDelayString = "${app.ingestion.scheduler.initial-delay:PT1M}")
    @Transactional
    public void runDueSources() {
        if (schedulingEnabled) {
            runDueSources(Instant.now());
        }
    }

    @Transactional
    public void runDueSources(Instant now) {
        if (!schedulingEnabled) {
            return;
        }
        List<UUID> sourceIds = sourceRepository
                .findByPermissionReviewStatusAndEnabledTrue(PermissionReviewStatus.APPROVED)
                .stream()
                .map(ScrapingSource::getId)
                .toList();
        for (UUID sourceId : sourceIds) {
            scheduleIfDue(sourceId, now);
        }
    }

    private void scheduleIfDue(UUID sourceId, Instant now) {
        ScrapingSource source = sourceRepository.findByIdForUpdate(sourceId).orElse(null);
        if (source == null || source.getPermissionReviewStatus() != PermissionReviewStatus.APPROVED || !source.isEnabled()) {
            return;
        }
        ScrapingRun lastAttempt = runRepository
                .findFirstBySource_IdAndStatusInOrderByStartedAtDesc(sourceId, ACTIVE_ATTEMPT_STATUSES)
                .orElse(null);
        if (lastAttempt != null && now.isBefore(lastAttempt.getStartedAt()
                .plus(Duration.ofMinutes(source.getCrawlIntervalMinutes())))) {
            return;
        }
        if (runRepository.existsBySource_IdAndStatus(sourceId, ScrapingRunStatus.RUNNING)) {
            recordScheduleDecision(source, ScrapingRunStatus.SKIPPED, now,
                    "Scheduled run skipped because another run is in progress");
            return;
        }
        if (lastAttempt != null) {
            Instant expectedAt = lastAttempt.getStartedAt().plus(Duration.ofMinutes(source.getCrawlIntervalMinutes()));
            if (!now.isBefore(expectedAt.plus(missedRunGrace))) {
                recordScheduleDecision(source, ScrapingRunStatus.MISSED, expectedAt,
                        "Scheduled run was missed because the scheduler did not run on time");
            }
        }
        try {
            startRun(source, null, ScrapingRunTrigger.SCHEDULED);
        } catch (RateLimitException ex) {
            recordScheduleDecision(source, ScrapingRunStatus.SKIPPED, now,
                    "Scheduled run skipped because the source rate limit is active");
        }
    }

    private void recordScheduleDecision(ScrapingSource source, ScrapingRunStatus status, Instant at, String summary) {
        ScrapingRun run = new ScrapingRun();
        run.setId(UUID.randomUUID());
        run.setSource(source);
        run.setStatus(status);
        run.setTriggerType(ScrapingRunTrigger.SCHEDULED);
        run.setStartedAt(at);
        run.setFinishedAt(at);
        run.setErrorSummary(summary);
        runRepository.save(run);
    }

    private ScrapingRunResponse startRun(ScrapingSource source, UUID retriedFromRunId,
                                         ScrapingRunTrigger triggerType) {
        if (source.getPermissionReviewStatus() != PermissionReviewStatus.APPROVED) {
            throw InvalidParameterException.of("sourceId", "only APPROVED sources can be ingested");
        }
        if (!source.isEnabled()) {
            throw InvalidParameterException.of("sourceId", "source must be enabled before ingestion");
        }
        if (runRepository.existsBySource_IdAndStatus(source.getId(), ScrapingRunStatus.RUNNING)) {
            throw new ConflictException("A scraping run is already in progress for this source");
        }
        enforceRateLimit(source);

        Instant now = Instant.now();
        ScrapingRun run = new ScrapingRun();
        run.setId(UUID.randomUUID());
        run.setSource(source);
        run.setStatus(ScrapingRunStatus.RUNNING);
        run.setTriggerType(triggerType);
        run.setStartedAt(now);
        run.setRetriedFromRunId(retriedFromRunId);
        try {
            runRepository.saveAndFlush(run);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A scraping run is already in progress for this source");
        }

        ScrapeSourceRequestedCommand command = new ScrapeSourceRequestedCommand(
                UUID.randomUUID(),
                "ScrapeSourceRequested",
                1,
                now,
                run.getId(),
                new ScrapeSourcePayload(
                        source.getId(),
                        source.getSourceName(),
                        source.getSourceType().name(),
                        source.getBaseUrl(),
                        source.getCompany() == null ? null : source.getCompany().getId(),
                        source.getRateLimitPerMinute()));
        try {
            commandPublisher.requestSourceScrape(command);
        } catch (AmqpException ex) {
            log.error("Could not publish scrape command for source {}", source.getId(), ex);
            throw new DependencyUnavailableException("The ingestion queue is unavailable");
        }
        return ScrapingRunResponse.from(run);
    }

    private void enforceRateLimit(ScrapingSource source) {
        Integer rateLimit = source.getRateLimitPerMinute();
        if (rateLimit == null || rateLimit <= 0) {
            return;
        }
        long minIntervalMillis = Math.max(1L, (60_000L + rateLimit - 1) / rateLimit);
        runRepository.findFirstBySource_IdAndStatusInOrderByStartedAtDesc(source.getId(), ACTIVE_ATTEMPT_STATUSES)
                .ifPresent(lastRun -> {
            Instant now = Instant.now();
            Instant nextAllowed = lastRun.getStartedAt().plusMillis(minIntervalMillis);
            if (now.isBefore(nextAllowed)) {
                long retryAfterSeconds = Math.max(1L, Duration.between(now, nextAllowed).toSeconds() + 1);
                throw new RateLimitException(
                        "Source rate limit reached; retry after " + retryAfterSeconds + " seconds",
                        retryAfterSeconds);
            }
        });
    }

    @Transactional
    public void completeRun(ScrapeRunCompletedEvent event) {
        if (event.eventId() == null || processedEventRepository.existsById(event.eventId())) {
            return;
        }
        ScrapingRun run = runRepository.findByIdForUpdate(event.runId())
                .orElseThrow(() -> new ResourceNotFoundException("Scraping run", event.runId()));
        if (!run.getSource().getId().equals(event.sourceId())) {
            throw new IllegalArgumentException("Run completion source does not match the run");
        }
        if (run.getFinishedAt() != null) {
            markProcessed(event.eventId(), "ScrapeRunCompleted");
            return;
        }

        int emitted = Math.max(0, event.recordsEmitted());
        int fetched = Math.max(emitted, event.recordsFetched());
        int unemitted = fetched - emitted;
        run.setRecordsEmitted(emitted);
        run.setRecordsDiscovered(Math.max(run.getRecordsDiscovered(), fetched));
        if (unemitted > 0) {
            run.setRecordsFailed(run.getRecordsFailed() + unemitted);
        }

        ScrapingRunStatus status;
        if ("FAILED".equalsIgnoreCase(event.status())) {
            status = ScrapingRunStatus.FAILED;
        } else if ("PARTIAL".equalsIgnoreCase(event.status())
                || run.getRecordsRejected() > 0
                || run.getRecordsFailed() > 0) {
            status = ScrapingRunStatus.PARTIAL;
        } else {
            status = ScrapingRunStatus.SUCCEEDED;
        }
        Instant now = Instant.now();
        run.setStatus(status);
        run.setFinishedAt(now);
        run.setErrorSummary(trimToNull(event.errorSummary()));
        ScrapingSource source = run.getSource();
        if (status == ScrapingRunStatus.FAILED) {
            source.setConsecutiveFailedRuns(source.getConsecutiveFailedRuns() + 1);
            source.setLastFailureAt(now);
        } else {
            source.setConsecutiveFailedRuns(0);
            source.setLastSuccessAt(now);
        }
        lifecycleService.reconcileCompletedRun(run);
        runRepository.save(run);
        markProcessed(event.eventId(), "ScrapeRunCompleted");
    }

    private void markProcessed(UUID eventId, String eventType) {
        ProcessedIngestionEvent processed = new ProcessedIngestionEvent();
        processed.setEventId(eventId);
        processed.setEventType(eventType);
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }

    private ScrapingRun findRun(UUID id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping run", id));
    }

    private Specification<ScrapingRun> toSpecification(UUID sourceId, ScrapingRunStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sourceId != null) {
                predicates.add(cb.equal(root.get("source").get("id"), sourceId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
