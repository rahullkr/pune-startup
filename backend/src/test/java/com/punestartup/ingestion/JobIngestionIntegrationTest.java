package com.punestartup.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.common.exception.ConflictException;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.GeocodeStatus;
import com.punestartup.company.entity.LocationType;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.ingestion.dto.ScrapedRecordDetailResponse;
import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.entity.ScrapingRun;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.ingestion.entity.ScrapingSourceType;
import com.punestartup.ingestion.event.JobCompanyPayload;
import com.punestartup.ingestion.event.JobDiscoveredEvent;
import com.punestartup.ingestion.event.JobPayload;
import com.punestartup.ingestion.event.ScrapeRunCompletedEvent;
import com.punestartup.ingestion.event.ScrapeSourceRequestedCommand;
import com.punestartup.ingestion.messaging.IngestionCommandPublisher;
import com.punestartup.ingestion.repository.ProcessedIngestionEventRepository;
import com.punestartup.ingestion.repository.ScrapedRecordRepository;
import com.punestartup.ingestion.repository.ScrapingRunRepository;
import com.punestartup.ingestion.repository.ScrapingSourceRepository;
import com.punestartup.ingestion.service.JobIngestionService;
import com.punestartup.ingestion.service.ScrapedRecordService;
import com.punestartup.ingestion.service.ScrapingRunService;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobSource;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSkillRepository;
import com.punestartup.job.repository.JobSourceRepository;
import com.punestartup.job.repository.JobStatusHistoryRepository;
import com.punestartup.skill.entity.Skill;
import com.punestartup.skill.repository.SkillRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "app.ingestion.freshness.stale-after-missing-checks=2",
        "app.ingestion.freshness.expire-after-missing-checks=3"
})
@Testcontainers
class JobIngestionIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @Autowired private JobIngestionService ingestionService;
    @Autowired private ScrapingRunService runService;
    @Autowired private ScrapedRecordService recordService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CompanyLocationRepository locationRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private ScrapingSourceRepository sourceRepository;
    @Autowired private ScrapingRunRepository scrapingRunRepository;
    @Autowired private ScrapedRecordRepository recordRepository;
    @Autowired private ProcessedIngestionEventRepository processedEventRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobSourceRepository jobSourceRepository;
    @Autowired private JobSkillRepository jobSkillRepository;
    @Autowired private JobStatusHistoryRepository statusHistoryRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private IngestionCommandPublisher commandPublisher;

    @BeforeEach
    void clean() {
        jdbcTemplate.execute("TRUNCATE TABLE processed_ingestion_events");
        jdbcTemplate.execute("TRUNCATE TABLE companies, skills CASCADE");
    }

    @Test
    void ingestsValidJobAsNeedsReviewWithSourceAndSkills() throws Exception {
        Company company = company("ZetaSoft");
        CompanyLocation location = location(company, "Baner");
        Skill java = skill("Java");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);

        JobDiscoveredEvent event = event(source, run, "ext-1", "Backend Engineer", "Baner, Pune",
                List.of("Java", "Kotlin"));

        ingestionService.ingest(event);

        assertThat(jobRepository.count()).isEqualTo(1);
        Job job = jobRepository.findAll().getFirst();
        assertThat(job.getStatus()).isEqualTo(JobStatus.NEEDS_REVIEW);
        assertThat(job.getNormalizedTitle()).isEqualTo("backend engineer");
        assertThat(job.getCompany().getId()).isEqualTo(company.getId());
        assertThat(job.getCompanyLocation().getId()).isEqualTo(location.getId());
        assertThat(job.getEmploymentType().name()).isEqualTo("FULL_TIME");
        assertThat(job.getWorkMode().name()).isEqualTo("HYBRID");
        assertThat(job.getSalaryMin()).isEqualByComparingTo("1000000");
        assertThat(jobSourceRepository.count()).isEqualTo(1);
        assertThat(jobSourceRepository.findAll().getFirst().getSourceJobId()).isEqualTo("ext-1");
        assertThat(jobSkillRepository.count()).isEqualTo(1);
        assertThat(jobSkillRepository.findAll().getFirst().getSkill().getId()).isEqualTo(java.getId());
        assertThat(recordRepository.count()).isEqualTo(1);
        assertThat(recordRepository.findAll().getFirst().getProcessingStatus()).isEqualTo(ScrapedRecordStatus.PROCESSED);
        ScrapingRun storedRun = scrapingRunRepository.findById(run.getId()).orElseThrow();
        assertThat(storedRun.getRecordsDiscovered()).isEqualTo(1);
        assertThat(storedRun.getRecordsValidated()).isEqualTo(1);
        assertThat(processedEventRepository.existsById(event.eventId())).isTrue();
    }

    @Test
    void reprocessingSameEventIsIdempotent() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);
        JobDiscoveredEvent event = event(source, run, "ext-1", "Backend Engineer", null, List.of());

        ingestionService.ingest(event);
        ingestionService.ingest(event);

        assertThat(jobRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(1);
        assertThat(scrapingRunRepository.findById(run.getId()).orElseThrow().getRecordsDiscovered()).isEqualTo(1);
    }

    @Test
    void detectsDuplicateSourceJobWithoutCreatingAnotherCanonicalJob() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);

        ingestionService.ingest(event(source, run, "ext-1", "Backend Engineer", null, List.of()));
        ingestionService.ingest(event(source, run, "ext-1", "Backend Engineer (Pune)", null, List.of()));

        assertThat(jobRepository.count()).isEqualTo(1);
        assertThat(recordRepository.count()).isEqualTo(2);
        assertThat(recordRepository.findAll()).anyMatch(r -> r.getProcessingStatus() == ScrapedRecordStatus.SKIPPED);
        ScrapingRun storedRun = scrapingRunRepository.findById(run.getId()).orElseThrow();
        assertThat(storedRun.getRecordsValidated()).isEqualTo(1);
        assertThat(storedRun.getRecordsDuplicated()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidSalaryButKeepsRawRecordForReview() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);
        JobDiscoveredEvent valid = event(source, run, "ext-2", "Data Analyst", null, List.of());
        JobPayload badPayload = new JobPayload(valid.job().title(), "Description", null, "FULL_TIME",
                "ONSITE", null, null, BigDecimal.valueOf(500000), null, "INR", "YEAR", List.of());
        JobDiscoveredEvent invalid = new JobDiscoveredEvent(UUID.randomUUID(), "JobDiscovered", 1,
                Instant.now(), "test", run.getId(), source.getId(), source.getSourceName(), "GREENHOUSE",
                "ext-2", "https://example.com/jobs/2", null, null, valid.company(), badPayload,
                objectMapper.readTree("{\"id\":\"ext-2\"}"));

        ingestionService.ingest(invalid);

        assertThat(jobRepository.count()).isZero();
        var record = recordRepository.findAll().getFirst();
        assertThat(record.getProcessingStatus()).isEqualTo(ScrapedRecordStatus.FAILED);
        assertThat(record.getErrorMessage()).contains("Salary requires both");
        assertThat(record.getPayload()).contains("ext-2");
        ScrapingRun storedRun = scrapingRunRepository.findById(run.getId()).orElseThrow();
        assertThat(storedRun.getRecordsRejected()).isEqualTo(1);
    }

    @Test
    void marksRecordFailedWhenCompanyCannotBeResolved() throws Exception {
        ScrapingSource source = source("Unlinked Board", null, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);
        JobDiscoveredEvent event = event(source, run, "ext-3", "Designer", null, List.of());

        ingestionService.ingest(event);

        assertThat(jobRepository.count()).isZero();
        var record = recordRepository.findAll().getFirst();
        assertThat(record.getProcessingStatus()).isEqualTo(ScrapedRecordStatus.FAILED);
        assertThat(record.getErrorMessage()).contains("Company could not be resolved");
    }

    @Test
    void completionFinalizesRunAndUpdatesSourceFreshness() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run, "ext-4", "Platform Engineer", null, List.of()));

        ScrapeRunCompletedEvent completed = new ScrapeRunCompletedEvent(UUID.randomUUID(), "ScrapeRunCompleted", 1,
                Instant.now(), "test", run.getId(), source.getId(), "SUCCEEDED", 1, 1, null);
        runService.completeRun(completed);
        runService.completeRun(completed);

        ScrapingRun stored = scrapingRunRepository.findById(run.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ScrapingRunStatus.SUCCEEDED);
        assertThat(stored.getFinishedAt()).isNotNull();
        assertThat(sourceRepository.findById(source.getId()).orElseThrow().getLastSuccessAt()).isNotNull();
    }

    @Test
    void lateJobEventDoesNotOverwriteCompletedRun() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run, "ext-6", "Platform Engineer", null, List.of()));

        ScrapeRunCompletedEvent completed = new ScrapeRunCompletedEvent(UUID.randomUUID(), "ScrapeRunCompleted", 1,
                Instant.now(), "test", run.getId(), source.getId(), "SUCCEEDED", 1, 1, null);
        runService.completeRun(completed);

        JobDiscoveredEvent valid = event(source, run, "ext-7", "Data Analyst", null, List.of());
        JobPayload badPayload = new JobPayload(valid.job().title(), "Description", null, "FULL_TIME",
                "ONSITE", null, null, BigDecimal.valueOf(500000), null, "INR", "YEAR", List.of());
        JobDiscoveredEvent late = new JobDiscoveredEvent(UUID.randomUUID(), "JobDiscovered", 1,
                Instant.now(), "test", run.getId(), source.getId(), source.getSourceName(), "GREENHOUSE",
                "ext-7", "https://example.com/jobs/7", null, null, valid.company(), badPayload,
                objectMapper.readTree("{\"id\":\"ext-7\"}"));
        ingestionService.ingest(late);

        ScrapingRun stored = scrapingRunRepository.findById(run.getId()).orElseThrow();
        assertThat(stored.getFinishedAt()).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(ScrapingRunStatus.PARTIAL);
        assertThat(stored.getRecordsDiscovered()).isEqualTo(2);
        assertThat(stored.getRecordsRejected()).isEqualTo(1);
    }

    @Test
    void tracksFirstSeenLastSeenAndLastCheckedAcrossSourceChecks() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run, "ext-f1", "Backend Engineer", null, List.of()));

        Job job = jobRepository.findAll().getFirst();
        JobSource sourceRecord = jobSourceRepository.findAll().getFirst();
        Instant firstSeen = job.getFirstSeenAt();
        Instant previousChecked = sourceRecord.getLastCheckedAt();
        assertThat(firstSeen).isNotNull();
        assertThat(previousChecked).isNotNull();
        assertThat(sourceRecord.getScrapingSourceId()).isEqualTo(source.getId());
        complete(run, source, "SUCCEEDED", 1, 1);

        pause();
        ScrapingRun run2 = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run2, "ext-f1", "Backend Engineer", null, List.of()));

        Job refreshedJob = jobRepository.findById(job.getId()).orElseThrow();
        JobSource refreshedSource = jobSourceRepository.findAll().getFirst();
        assertThat(refreshedJob.getFirstSeenAt()).isEqualTo(firstSeen);
        assertThat(refreshedJob.getLastSeenAt()).isAfter(previousChecked);
        assertThat(refreshedJob.getLastCheckedAt()).isAfter(previousChecked);
        assertThat(refreshedSource.getLastSeenAt()).isAfter(previousChecked);
        assertThat(refreshedSource.getLastCheckedAt()).isAfter(previousChecked);
        assertThat(refreshedSource.getConsecutiveMissingChecks()).isZero();
        assertThat(refreshedSource.getSourceStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void marksStaleAndExpiresAfterConsecutiveMissingSuccessfulChecks() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run1 = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run1, "ext-l1", "Backend Engineer", null, List.of()));
        recordService.publish(recordRepository.findAll().getFirst().getId());
        complete(run1, source, "SUCCEEDED", 1, 1);

        Job job = jobRepository.findAll().getFirst();
        assertThat(job.getStatus()).isEqualTo(JobStatus.ACTIVE);
        assertThat(jobSourceRepository.findAll().getFirst().getConsecutiveMissingChecks()).isZero();

        ScrapingRun run2 = emptyRun(source);
        complete(run2, source, "SUCCEEDED", 0, 0);
        JobSource afterFirstMiss = jobSourceRepository.findAll().getFirst();
        assertThat(afterFirstMiss.getConsecutiveMissingChecks()).isEqualTo(1);
        assertThat(afterFirstMiss.getSourceStatus()).isEqualTo("MISSING");
        assertThat(afterFirstMiss.getLastMissingRunId()).isEqualTo(run2.getId());
        assertThat(reloadJob(job).getStatus()).isEqualTo(JobStatus.ACTIVE);

        ScrapingRun run3 = emptyRun(source);
        complete(run3, source, "SUCCEEDED", 0, 0);
        JobSource afterSecondMiss = jobSourceRepository.findAll().getFirst();
        assertThat(afterSecondMiss.getConsecutiveMissingChecks()).isEqualTo(2);
        assertThat(afterSecondMiss.getSourceStatus()).isEqualTo("REMOVED");
        assertThat(afterSecondMiss.getRemovedAt()).isNotNull();
        Job stale = reloadJob(job);
        assertThat(stale.getStatus()).isEqualTo(JobStatus.STALE);
        assertThat(stale.getStaleSince()).isNotNull();
        assertThat(statusHistoryRepository.findByJob_IdOrderByChangedAtAsc(job.getId()))
                .anyMatch(history -> history.getNewStatus() == JobStatus.STALE
                        && run3.getId().equals(history.getScrapingRunId())
                        && history.getReason().contains("missing checks"));

        ScrapingRun run4 = emptyRun(source);
        complete(run4, source, "SUCCEEDED", 0, 0);
        Job expired = reloadJob(job);
        assertThat(expired.getStatus()).isEqualTo(JobStatus.EXPIRED);
        assertThat(expired.getExpiredAt()).isNotNull();
        assertThat(statusHistoryRepository.findByJob_IdOrderByChangedAtAsc(job.getId()))
                .anyMatch(history -> history.getNewStatus() == JobStatus.EXPIRED
                        && "Removed after 3 consecutive missing checks".equals(history.getReason()));
    }

    @Test
    void reappearingJobReturnsToActiveAndRecordsTheTransition() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run1 = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run1, "ext-r1", "Backend Engineer", null, List.of()));
        recordService.publish(recordRepository.findAll().getFirst().getId());
        complete(run1, source, "SUCCEEDED", 1, 1);
        Job job = jobRepository.findAll().getFirst();

        ScrapingRun run2 = emptyRun(source);
        complete(run2, source, "SUCCEEDED", 0, 0);
        ScrapingRun run3 = emptyRun(source);
        complete(run3, source, "SUCCEEDED", 0, 0);
        assertThat(reloadJob(job).getStatus()).isEqualTo(JobStatus.STALE);

        pause();
        ScrapingRun run4 = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run4, "ext-r1", "Backend Engineer", null, List.of()));

        Job reactivated = reloadJob(job);
        assertThat(reactivated.getStatus()).isEqualTo(JobStatus.ACTIVE);
        assertThat(reactivated.getStaleSince()).isNull();
        JobSource sourceRecord = jobSourceRepository.findAll().getFirst();
        assertThat(sourceRecord.getSourceStatus()).isEqualTo("ACTIVE");
        assertThat(sourceRecord.getConsecutiveMissingChecks()).isZero();
        assertThat(statusHistoryRepository.findByJob_IdOrderByChangedAtAsc(job.getId()))
                .anyMatch(history -> history.getNewStatus() == JobStatus.ACTIVE
                        && history.getReason().contains("confirms the listing again"));

        complete(run4, source, "SUCCEEDED", 1, 1);
        assertThat(reloadJob(job).getStatus()).isEqualTo(JobStatus.ACTIVE);
    }

    @Test
    void failedRunDoesNotMarkKnownJobsMissing() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run1 = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run1, "ext-x1", "Backend Engineer", null, List.of()));
        recordService.publish(recordRepository.findAll().getFirst().getId());
        complete(run1, source, "SUCCEEDED", 1, 1);
        Job job = jobRepository.findAll().getFirst();

        ScrapingRun failed = emptyRun(source);
        complete(failed, source, "FAILED", 0, 0);

        assertThat(reloadJob(job).getStatus()).isEqualTo(JobStatus.ACTIVE);
        JobSource sourceRecord = jobSourceRepository.findAll().getFirst();
        assertThat(sourceRecord.getConsecutiveMissingChecks()).isZero();
        assertThat(sourceRecord.getSourceStatus()).isEqualTo("ACTIVE");
        assertThat(sourceRecord.getLastMissingAt()).isNull();
    }

    @Test
    void defersMissingReconciliationUntilAllEmittedRecordsAreProcessed() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run1 = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run1, "ext-d1", "Backend Engineer", null, List.of()));
        recordService.publish(recordRepository.findAll().getFirst().getId());
        complete(run1, source, "SUCCEEDED", 1, 1);
        Job job = jobRepository.findAll().getFirst();

        pause();
        ScrapingRun run2 = run(source, ScrapingRunStatus.RUNNING);
        complete(run2, source, "SUCCEEDED", 1, 1);

        assertThat(jobSourceRepository.findAll().getFirst().getConsecutiveMissingChecks()).isZero();
        assertThat(scrapingRunRepository.findById(run2.getId()).orElseThrow()
                .getFreshnessReconciledAt()).isNull();

        ingestionService.ingest(event(source, run2, "ext-d1", "Backend Engineer", null, List.of()));

        assertThat(jobSourceRepository.findAll().getFirst().getConsecutiveMissingChecks()).isZero();
        assertThat(scrapingRunRepository.findById(run2.getId()).orElseThrow()
                .getFreshnessReconciledAt()).isNotNull();
        assertThat(reloadJob(job).getStatus()).isEqualTo(JobStatus.ACTIVE);
    }

    @Test
    void adminCanPublishAndRejectReviewedJobs() throws Exception {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);
        ScrapingRun run = run(source, ScrapingRunStatus.RUNNING);
        ingestionService.ingest(event(source, run, "ext-5", "Frontend Engineer", null, List.of()));

        UUID recordId = recordRepository.findAll().getFirst().getId();
        ScrapedRecordDetailResponse published = recordService.publish(recordId);
        assertThat(published.jobStatus()).isEqualTo(JobStatus.ACTIVE);
        assertThat(published.processingStatus()).isEqualTo(ScrapedRecordStatus.PROCESSED);

        ScrapedRecordDetailResponse rejected = recordService.reject(recordId);
        assertThat(rejected.jobStatus()).isEqualTo(JobStatus.REJECTED);
        assertThat(rejected.processingStatus()).isEqualTo(ScrapedRecordStatus.SKIPPED);

        UUID jobId = recordRepository.findById(recordId).orElseThrow().getJob().getId();
        assertThat(statusHistoryRepository.findByJob_IdOrderByChangedAtAsc(jobId))
                .extracting(history -> history.getNewStatus())
                .containsExactly(JobStatus.ACTIVE, JobStatus.REJECTED);
    }

    @Test
    void manualRunRequiresApprovedEnabledSourceAndPublishesCommand() {
        Company company = company("ZetaSoft");
        ScrapingSource source = source("Greenhouse Zeta", company, PermissionReviewStatus.APPROVED, true);

        var run = runService.startRun(source.getId());
        assertThat(run.status()).isEqualTo(ScrapingRunStatus.RUNNING);
        ArgumentCaptor<ScrapeSourceRequestedCommand> captor = ArgumentCaptor.forClass(ScrapeSourceRequestedCommand.class);
        verify(commandPublisher).requestSourceScrape(captor.capture());
        assertThat(captor.getValue().runId()).isEqualTo(run.id());
        assertThat(captor.getValue().source().sourceId()).isEqualTo(source.getId());

        assertThatThrownBy(() -> runService.startRun(source.getId())).isInstanceOf(ConflictException.class);

        ScrapingSource pending = source("Lever Pending", company, PermissionReviewStatus.PENDING, false);
        assertThatThrownBy(() -> runService.startRun(pending.getId())).isInstanceOf(InvalidParameterException.class);
    }

    private ScrapingRun emptyRun(ScrapingSource source) {
        pause();
        return run(source, ScrapingRunStatus.RUNNING);
    }

    private void complete(ScrapingRun run, ScrapingSource source, String status, int fetched, int emitted) {
        ScrapeRunCompletedEvent completed = new ScrapeRunCompletedEvent(UUID.randomUUID(), "ScrapeRunCompleted", 1,
                Instant.now(), "test", run.getId(), source.getId(), status, fetched, emitted, null);
        runService.completeRun(completed);
    }

    private Job reloadJob(Job job) {
        return jobRepository.findById(job.getId()).orElseThrow();
    }

    private void pause() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private Company company(String name) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setNormalizedName(name.toLowerCase());
        company.setSlug(name.toLowerCase().replace(' ', '-'));
        company.setCompanyType(CompanyType.STARTUP);
        company.setVerificationStatus(VerificationStatus.VERIFIED);
        company.setRecordStatus(RecordStatus.ACTIVE);
        return companyRepository.save(company);
    }

    private CompanyLocation location(Company company, String area) {
        CompanyLocation location = new CompanyLocation();
        location.setId(UUID.randomUUID());
        location.setCompany(company);
        location.setLocationName(company.getName() + " " + area + " Office");
        location.setArea(area);
        location.setCity("Pune");
        location.setState("Maharashtra");
        location.setCountryCode("IN");
        location.setLocationType(LocationType.OFFICE);
        location.setGeocodeStatus(GeocodeStatus.VERIFIED);
        location.setHeadquarters(true);
        return locationRepository.save(location);
    }

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID());
        skill.setName(name);
        skill.setNormalizedName(name.toLowerCase());
        skill.setCreatedAt(Instant.now());
        return skillRepository.save(skill);
    }

    private ScrapingSource source(String name, Company company, PermissionReviewStatus status, boolean enabled) {
        ScrapingSource source = new ScrapingSource();
        source.setId(UUID.randomUUID());
        source.setSourceName(name);
        source.setSourceType(ScrapingSourceType.GREENHOUSE);
        source.setBaseUrl("https://boards.example.com/" + UUID.randomUUID());
        source.setCompany(company);
        source.setPermissionReviewStatus(status);
        source.setCrawlIntervalMinutes(360);
        source.setRateLimitPerMinute(30);
        source.setEnabled(enabled);
        return sourceRepository.save(source);
    }

    private ScrapingRun run(ScrapingSource source, ScrapingRunStatus status) {
        ScrapingRun run = new ScrapingRun();
        run.setId(UUID.randomUUID());
        run.setSource(source);
        run.setStatus(status);
        run.setStartedAt(Instant.now());
        return scrapingRunRepository.save(run);
    }

    private JobDiscoveredEvent event(ScrapingSource source, ScrapingRun run, String externalId,
                                     String title, String locationText, List<String> skills) throws Exception {
        JsonNode raw = objectMapper.readTree("{\"id\":\"" + externalId + "\"}");
        JobPayload payload = new JobPayload(
                title,
                "Build reliable software.",
                locationText,
                "FULL_TIME",
                "HYBRID",
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(1500000),
                "INR",
                "YEAR",
                skills);
        return new JobDiscoveredEvent(
                UUID.randomUUID(),
                "JobDiscovered",
                1,
                Instant.now(),
                "test",
                run.getId(),
                source.getId(),
                source.getSourceName(),
                source.getSourceType().name(),
                externalId,
                "https://example.com/jobs/" + externalId,
                "https://example.com/jobs/" + externalId + "/apply",
                Instant.now(),
                new JobCompanyPayload("ZetaSoft", "https://zetasoft.example", "Pune"),
                payload,
                raw);
    }
}
