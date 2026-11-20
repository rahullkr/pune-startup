package com.punestartup.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.util.TextNormalizer;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ProcessedIngestionEvent;
import com.punestartup.ingestion.entity.ScrapedRecord;
import com.punestartup.ingestion.entity.ScrapedRecordStatus;
import com.punestartup.ingestion.entity.ScrapedRecordType;
import com.punestartup.ingestion.entity.ScrapingRun;
import com.punestartup.ingestion.entity.ScrapingRunStatus;
import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.ingestion.event.JobDiscoveredEvent;
import com.punestartup.ingestion.event.JobPayload;
import com.punestartup.ingestion.repository.ProcessedIngestionEventRepository;
import com.punestartup.ingestion.repository.ScrapedRecordRepository;
import com.punestartup.ingestion.repository.ScrapingRunRepository;
import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobSkill;
import com.punestartup.job.entity.JobSkillId;
import com.punestartup.job.entity.JobSource;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.SkillRequirementType;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSkillRepository;
import com.punestartup.job.repository.JobSourceRepository;
import com.punestartup.skill.entity.Skill;
import com.punestartup.skill.repository.SkillRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobIngestionService {

    private static final Logger log = LoggerFactory.getLogger(JobIngestionService.class);

    private final ScrapedRecordRepository recordRepository;
    private final ScrapingRunRepository runRepository;
    private final ProcessedIngestionEventRepository processedEventRepository;
    private final CompanyRepository companyRepository;
    private final CompanyLocationRepository locationRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;

    public JobIngestionService(ScrapedRecordRepository recordRepository,
                               ScrapingRunRepository runRepository,
                               ProcessedIngestionEventRepository processedEventRepository,
                               CompanyRepository companyRepository,
                               CompanyLocationRepository locationRepository,
                               SkillRepository skillRepository,
                               JobRepository jobRepository,
                               JobSourceRepository jobSourceRepository,
                               JobSkillRepository jobSkillRepository,
                               JobLifecycleService lifecycleService,
                               ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.runRepository = runRepository;
        this.processedEventRepository = processedEventRepository;
        this.companyRepository = companyRepository;
        this.locationRepository = locationRepository;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.lifecycleService = lifecycleService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ingest(JobDiscoveredEvent event) {
        if (event.eventId() == null) {
            throw new IllegalArgumentException("JobDiscovered event is missing eventId");
        }
        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Skipping already processed ingestion event {}", event.eventId());
            return;
        }
        if (!"JobDiscovered".equals(event.eventType()) || event.schemaVersion() != 1) {
            throw new IllegalArgumentException("Unsupported ingestion event type or schema version");
        }
        if (event.runId() == null || event.sourceId() == null) {
            throw new IllegalArgumentException("JobDiscovered event is missing runId or sourceId");
        }

        ScrapingRun run = runRepository.findByIdForUpdate(event.runId())
                .orElseThrow(() -> new ResourceNotFoundException("Scraping run", event.runId()));
        ScrapingSource source = run.getSource();
        if (!source.getId().equals(event.sourceId())) {
            throw new IllegalArgumentException("Event sourceId does not match the scraping run");
        }

        Instant now = Instant.now();
        ScrapedRecord record = new ScrapedRecord();
        record.setId(UUID.randomUUID());
        record.setRun(run);
        record.setRecordType(ScrapedRecordType.JOB);
        record.setExternalId(trimToNull(event.externalJobId()));
        record.setSourceUrl(trimToNull(event.sourceUrl()));
        record.setPayload(writeJson(event.rawPayload() == null ? event : event.rawPayload()));
        record.setNormalizedPayload(event.job() == null ? null : writeJson(event.job()));
        record.setProcessingStatus(ScrapedRecordStatus.PENDING);
        record.setCreatedAt(now);
        record.setEventId(event.eventId());
        recordRepository.save(record);

        run.setRecordsDiscovered(run.getRecordsDiscovered() + 1);
        run.setRecordsProcessed(run.getRecordsProcessed() + 1);
        try {
            if (source.getPermissionReviewStatus() != PermissionReviewStatus.APPROVED || !source.isEnabled()) {
                throw new RecordValidationException("Source is not approved and enabled");
            }
            ValidatedJob validated = validate(event, now);
            Company company = resolveCompany(source, event);
            CompanyLocation location = resolveLocation(company, validated.locationText());
            List<Skill> skills = resolveSkills(validated.skills());

            Optional<JobSource> duplicate = findDuplicate(source, event, company, location, validated.normalizedTitle());
            if (duplicate.isPresent()) {
                JobSource existing = duplicate.get();
                touchDuplicate(existing, event, run, record, now);
                run.setRecordsDuplicated(run.getRecordsDuplicated() + 1);
            } else {
                createJob(source, event, validated, company, location, skills, record, now);
                run.setRecordsValidated(run.getRecordsValidated() + 1);
            }
        } catch (RecordValidationException ex) {
            record.setProcessingStatus(ScrapedRecordStatus.FAILED);
            record.setErrorMessage(ex.getMessage());
            record.setProcessedAt(now);
            run.setRecordsRejected(run.getRecordsRejected() + 1);
        }

        recordRepository.save(record);
        refreshCompletedStatus(run);
        lifecycleService.reconcileCompletedRun(run);
        runRepository.save(run);
        markProcessed(event.eventId(), "JobDiscovered");
    }

    private void refreshCompletedStatus(ScrapingRun run) {
        if (run.getFinishedAt() == null || run.getStatus() == ScrapingRunStatus.FAILED) {
            return;
        }
        run.setStatus(run.getRecordsRejected() > 0 || run.getRecordsFailed() > 0
                ? ScrapingRunStatus.PARTIAL
                : ScrapingRunStatus.SUCCEEDED);
    }

    private ValidatedJob validate(JobDiscoveredEvent event, Instant now) {
        if (event.job() == null) {
            throw new RecordValidationException("Job payload is required");
        }
        JobPayload payload = event.job();
        String title = trimToNull(payload.title());
        if (title == null) {
            throw new RecordValidationException("Job title is required");
        }
        if (title.length() > 250) {
            throw new RecordValidationException("Job title exceeds 250 characters");
        }
        String sourceUrl = trimToNull(event.sourceUrl());
        validateUrl(sourceUrl, "sourceUrl", true);
        validateUrl(trimToNull(event.applicationUrl()), "applicationUrl", false);

        EmploymentType employmentType;
        try {
            employmentType = EmploymentType.valueOf(normalizeEnum(payload.employmentType()));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new RecordValidationException("Unsupported employmentType: " + payload.employmentType());
        }
        WorkMode workMode = parseWorkMode(payload.workMode());

        BigDecimal minExperience = payload.experienceMinYears();
        BigDecimal maxExperience = payload.experienceMaxYears();
        validateRange(minExperience, maxExperience, "experience");
        BigDecimal salaryMin = payload.salaryMin();
        BigDecimal salaryMax = payload.salaryMax();
        String currency = trimToNull(payload.salaryCurrency());
        String period = trimToNull(payload.salaryPeriod());
        if (salaryMin != null || salaryMax != null) {
            if (salaryMin == null || salaryMax == null) {
                throw new RecordValidationException("Salary requires both minimum and maximum bounds");
            }
            validateRange(salaryMin, salaryMax, "salary");
            if (currency == null || !currency.matches("[A-Za-z]{3}")) {
                throw new RecordValidationException("Salary currency must be a three-letter code");
            }
            if (period == null) {
                throw new RecordValidationException("Salary period is required when salary is present");
            }
            currency = currency.toUpperCase(Locale.ROOT);
            period = period.toUpperCase(Locale.ROOT);
        } else {
            currency = null;
            period = null;
        }

        List<String> skills = payload.skills() == null ? List.of() : payload.skills().stream()
                .map(this::trimToNull)
                .filter(value -> value != null && !value.isBlank())
                .toList();

        return new ValidatedJob(
                title,
                TextNormalizer.normalizeName(title),
                trimToNull(payload.description()),
                trimToNull(payload.locationText()),
                employmentType,
                workMode,
                minExperience,
                maxExperience,
                salaryMin,
                salaryMax,
                currency,
                period,
                skills,
                sourceUrl,
                trimToNull(event.applicationUrl()),
                event.postedAt() == null ? now : event.postedAt());
    }

    private Company resolveCompany(ScrapingSource source, JobDiscoveredEvent event) {
        if (source.getCompany() != null) {
            return source.getCompany();
        }
        if (event.company() != null) {
            String name = TextNormalizer.normalizeName(event.company().name());
            if (name != null && !name.isBlank()) {
                Optional<Company> match = companyRepository.findFirstByNormalizedName(name);
                if (match.isPresent()) {
                    return match.get();
                }
            }
            String website = trimToNull(event.company().websiteUrl());
            if (website != null) {
                Optional<Company> match = companyRepository.findFirstByWebsiteUrlIgnoreCase(website);
                if (match.isPresent()) {
                    return match.get();
                }
            }
        }
        throw new RecordValidationException("Company could not be resolved; link the source to a company");
    }

    private CompanyLocation resolveLocation(Company company, String locationText) {
        String text = TextNormalizer.normalizeName(locationText);
        if (text == null || text.isBlank()) {
            return null;
        }
        for (CompanyLocation location : locationRepository.findByCompanyIdOrderByHeadquartersDesc(company.getId())) {
            if (matches(text, location.getArea())
                    || matches(text, location.getLocationName())
                    || matches(text, location.getCity())
                    || matches(text, location.getAddressLine())) {
                return location;
            }
        }
        return null;
    }

    private boolean matches(String text, String candidate) {
        String normalized = TextNormalizer.normalizeName(candidate);
        return normalized != null && !normalized.isBlank()
                && (text.contains(normalized) || normalized.contains(text));
    }

    private List<Skill> resolveSkills(List<String> skills) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String skill : skills) {
            String name = TextNormalizer.normalizeName(skill);
            if (name != null && !name.isBlank()) {
                normalized.add(name);
            }
        }
        List<Skill> resolved = new ArrayList<>();
        for (String name : normalized) {
            skillRepository.findByNormalizedName(name).ifPresent(resolved::add);
        }
        return resolved;
    }

    private Optional<JobSource> findDuplicate(ScrapingSource source, JobDiscoveredEvent event, Company company,
                                              CompanyLocation location, String normalizedTitle) {
        String externalId = trimToNull(event.externalJobId());
        if (externalId != null) {
            Optional<JobSource> exact = jobSourceRepository
                    .findFirstBySourceNameAndSourceJobId(source.getSourceName(), externalId);
            if (exact.isPresent()) {
                return exact;
            }
        }
        String applicationUrl = trimToNull(event.applicationUrl());
        if (applicationUrl != null) {
            List<JobSource> urlMatches = jobSourceRepository.findByCanonicalUrl(applicationUrl);
            if (!urlMatches.isEmpty()) {
                return Optional.of(urlMatches.getFirst());
            }
        }
        String sourceUrl = trimToNull(event.sourceUrl());
        if (sourceUrl != null) {
            List<JobSource> urlMatches = jobSourceRepository.findByCanonicalUrl(sourceUrl);
            if (!urlMatches.isEmpty()) {
                return Optional.of(urlMatches.getFirst());
            }
        }
        if (location == null) {
            return jobSourceRepository
                    .findFirstByJob_Company_IdAndJob_NormalizedTitleAndJob_CompanyLocationIsNullOrderByFirstSeenAtAsc(
                            company.getId(), normalizedTitle);
        }
        return jobSourceRepository
                .findFirstByJob_Company_IdAndJob_NormalizedTitleAndJob_CompanyLocation_IdOrderByFirstSeenAtAsc(
                        company.getId(), normalizedTitle, location.getId());
    }

    private void touchDuplicate(JobSource existing, JobDiscoveredEvent event, ScrapingRun run,
                                ScrapedRecord record, Instant now) {
        existing.setLastSeenAt(now);
        existing.setLastCheckedAt(now);
        existing.setConsecutiveMissingChecks(0);
        existing.setSourceStatus("ACTIVE");
        existing.setRawMetadata(writeJson(event.rawPayload()));
        if (existing.getApplicationUrl() == null && trimToNull(event.applicationUrl()) != null) {
            existing.setApplicationUrl(trimToNull(event.applicationUrl()));
        }
        if (existing.getScrapingSourceId() == null
                && existing.getSourceName().equalsIgnoreCase(event.sourceName())) {
            existing.setScrapingSourceId(event.sourceId());
        }
        jobSourceRepository.save(existing);
        lifecycleService.onJobConfirmed(existing.getJob(), existing, run, now);

        record.setProcessingStatus(ScrapedRecordStatus.SKIPPED);
        record.setJob(existing.getJob());
        record.setErrorMessage("Duplicate of existing job " + existing.getJob().getId());
        record.setProcessedAt(now);
    }

    private void createJob(ScrapingSource source, JobDiscoveredEvent event, ValidatedJob validated, Company company,
                           CompanyLocation location, List<Skill> skills, ScrapedRecord record, Instant now) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setCompany(company);
        job.setCompanyLocation(location);
        job.setTitle(validated.title());
        job.setNormalizedTitle(validated.normalizedTitle());
        job.setDescription(validated.description());
        job.setEmploymentType(validated.employmentType());
        job.setWorkMode(validated.workMode());
        job.setExperienceMinYears(validated.experienceMinYears());
        job.setExperienceMaxYears(validated.experienceMaxYears());
        job.setSalaryMin(validated.salaryMin());
        job.setSalaryMax(validated.salaryMax());
        job.setSalaryCurrency(validated.salaryCurrency());
        job.setSalaryPeriod(validated.salaryPeriod());
        job.setStatus(JobStatus.NEEDS_REVIEW);
        job.setFirstPublishedAt(validated.postedAt());
        job.setFirstSeenAt(now);
        job.setLastSeenAt(now);
        job.setLastCheckedAt(now);
        job.setLastVerifiedAt(now);
        jobRepository.save(job);

        for (Skill skill : skills) {
            JobSkill jobSkill = new JobSkill();
            jobSkill.setId(new JobSkillId(job.getId(), skill.getId()));
            jobSkill.setJob(job);
            jobSkill.setSkill(skill);
            jobSkill.setRequirementType(SkillRequirementType.REQUIRED);
            jobSkillRepository.save(jobSkill);
        }

        JobSource jobSource = new JobSource();
        jobSource.setId(UUID.randomUUID());
        jobSource.setJob(job);
        jobSource.setSourceName(source.getSourceName());
        jobSource.setSourceJobId(trimToNull(event.externalJobId()));
        jobSource.setSourceUrl(validated.sourceUrl());
        jobSource.setApplicationUrl(validated.applicationUrl());
        jobSource.setSourceStatus("ACTIVE");
        jobSource.setFirstSeenAt(now);
        jobSource.setLastSeenAt(now);
        jobSource.setLastCheckedAt(now);
        jobSource.setConsecutiveMissingChecks(0);
        jobSource.setScrapingSourceId(source.getId());
        jobSource.setRawMetadata(writeJson(event.rawPayload()));
        jobSourceRepository.save(jobSource);

        record.setProcessingStatus(ScrapedRecordStatus.PROCESSED);
        record.setJob(job);
        record.setProcessedAt(now);
    }

    private void validateUrl(String value, String field, boolean required) {
        if (value == null) {
            if (required) {
                throw new RecordValidationException(field + " is required");
            }
            return;
        }
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null
                    || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("not an absolute http(s) URL");
            }
        } catch (IllegalArgumentException ex) {
            throw new RecordValidationException(field + " must be an absolute http(s) URL");
        }
    }

    private void validateRange(BigDecimal min, BigDecimal max, String field) {
        if (min != null && min.signum() < 0) {
            throw new RecordValidationException(field + " minimum must be non-negative");
        }
        if (max != null && max.signum() < 0) {
            throw new RecordValidationException(field + " maximum must be non-negative");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new RecordValidationException(field + " minimum must not exceed maximum");
        }
    }

    private WorkMode parseWorkMode(String value) {
        String normalized = normalizeEnum(value);
        if (normalized == null) {
            return WorkMode.UNKNOWN;
        }
        try {
            return WorkMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return WorkMode.UNKNOWN;
        }
    }

    private String normalizeEnum(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.isEmpty() ? null : normalized;
    }

    private void markProcessed(UUID eventId, String eventType) {
        ProcessedIngestionEvent processed = new ProcessedIngestionEvent();
        processed.setEventId(eventId);
        processed.setEventType(eventType);
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize ingestion payload", ex);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ValidatedJob(
            String title,
            String normalizedTitle,
            String description,
            String locationText,
            EmploymentType employmentType,
            WorkMode workMode,
            BigDecimal experienceMinYears,
            BigDecimal experienceMaxYears,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            String salaryPeriod,
            List<String> skills,
            String sourceUrl,
            String applicationUrl,
            Instant postedAt) {
    }
}
