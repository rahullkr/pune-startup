package com.punestartup.job.service;

import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.util.TextNormalizer;
import com.punestartup.common.web.PageRequests;
import com.punestartup.company.dto.CompanyLocationResponse;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.ingestion.service.JobLifecycleService;
import com.punestartup.job.dto.AdminJobDetailResponse;
import com.punestartup.job.dto.AdminJobSourceResponse;
import com.punestartup.job.dto.AdminJobSummaryResponse;
import com.punestartup.job.dto.CreateJobRequest;
import com.punestartup.job.dto.FlagDuplicateRequest;
import com.punestartup.job.dto.JobCompanyResponse;
import com.punestartup.job.dto.JobSkillInput;
import com.punestartup.job.dto.JobSkillResponse;
import com.punestartup.job.dto.JobSourceInput;
import com.punestartup.job.dto.JobStatusHistoryResponse;
import com.punestartup.job.dto.MergeJobsRequest;
import com.punestartup.job.dto.UpdateJobRequest;
import com.punestartup.job.dto.UpdateJobSourceRequest;
import com.punestartup.job.entity.EmploymentType;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobSkill;
import com.punestartup.job.entity.JobSkillId;
import com.punestartup.job.entity.JobSource;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.entity.JobStatusHistory;
import com.punestartup.job.entity.SkillRequirementType;
import com.punestartup.job.entity.WorkMode;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSkillRepository;
import com.punestartup.job.repository.JobSourceRepository;
import com.punestartup.job.repository.JobStatusHistoryRepository;
import com.punestartup.skill.entity.Skill;
import com.punestartup.skill.repository.SkillRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminJobService {

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobStatusHistoryRepository statusHistoryRepository;
    private final CompanyRepository companyRepository;
    private final CompanyLocationRepository locationRepository;
    private final SkillRepository skillRepository;
    private final JobLifecycleService lifecycleService;
    private final int maxPageSize;

    public AdminJobService(JobRepository jobRepository,
                           JobSourceRepository jobSourceRepository,
                           JobSkillRepository jobSkillRepository,
                           JobStatusHistoryRepository statusHistoryRepository,
                           CompanyRepository companyRepository,
                           CompanyLocationRepository locationRepository,
                           SkillRepository skillRepository,
                           JobLifecycleService lifecycleService,
                           @Value("${app.search.max-page-size}") int maxPageSize) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.companyRepository = companyRepository;
        this.locationRepository = locationRepository;
        this.skillRepository = skillRepository;
        this.lifecycleService = lifecycleService;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<AdminJobSummaryResponse> listJobs(String keyword, UUID companyId, JobStatus status,
                                                  EmploymentType employmentType, WorkMode workMode,
                                                  String sort, int page, int size) {
        return jobRepository.findAll(toSpecification(keyword, companyId, status, employmentType, workMode),
                        PageRequests.of(page, size, sort(sort), maxPageSize))
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public AdminJobDetailResponse getJob(UUID id) {
        return toDetail(findJob(id));
    }

    @Transactional(readOnly = true)
    public List<AdminJobSummaryResponse> findDuplicates(UUID companyId, String title, String sourceUrl,
                                                        String applicationUrl, UUID excludeId) {
        String normalizedTitle = TextNormalizer.normalizeName(title);
        String source = trimToNull(sourceUrl);
        String application = trimToNull(applicationUrl);

        Map<UUID, Job> matches = new LinkedHashMap<>();
        if (source != null) {
            addUrlMatches(source, excludeId, matches);
        }
        if (application != null) {
            addUrlMatches(application, excludeId, matches);
        }
        if (companyId != null && normalizedTitle != null && !normalizedTitle.isBlank()) {
            jobRepository.findTop20ByCompany_IdAndNormalizedTitleOrderByCreatedAtDesc(companyId, normalizedTitle)
                    .stream()
                    .filter(job -> !job.getId().equals(excludeId))
                    .forEach(job -> matches.put(job.getId(), job));
        }
        if (source == null && application == null && (companyId == null || normalizedTitle == null)) {
            throw InvalidParameterException.of("companyId",
                    "provide companyId with title, sourceUrl, or applicationUrl to check duplicates");
        }
        return matches.values().stream()
                .limit(20)
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public AdminJobDetailResponse createJob(CreateJobRequest request) {
        if (request.companyId() == null) {
            throw InvalidParameterException.of("companyId", "must not be null");
        }
        Company company = findCompany(request.companyId());
        CompanyLocation location = resolveLocation(company, request.companyLocationId());

        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setCompany(company);
        job.setCompanyLocation(location);
        String title = requireText(request.title(), "title");
        job.setTitle(title);
        job.setNormalizedTitle(TextNormalizer.normalizeName(title));
        job.setDescription(trimToNull(request.description()));
        job.setEmploymentType(request.employmentType() == null ? EmploymentType.FULL_TIME : request.employmentType());
        job.setWorkMode(request.workMode() == null ? WorkMode.UNKNOWN : request.workMode());
        applyExperience(job, request.experienceMinYears(), request.experienceMaxYears());
        applySalary(job, request.salaryMin(), request.salaryMax(), request.salaryCurrency(),
                request.salaryPeriod());
        job.setExpiresAt(request.expiresAt());
        job.setStatus(JobStatus.NEEDS_REVIEW);

        Instant now = Instant.now();
        job.setFirstSeenAt(now);
        job.setLastSeenAt(now);
        job.setLastCheckedAt(now);
        job = jobRepository.saveAndFlush(job);

        replaceSkills(job, request.skills() == null ? List.of() : request.skills());
        if (request.source() != null) {
            jobSourceRepository.save(buildSource(job, request.source(), now));
        }
        return toDetail(job);
    }

    @Transactional
    public AdminJobDetailResponse updateJob(UUID id, UpdateJobRequest request) {
        Job job = findJob(id);
        Company company = job.getCompany();

        if (request.companyId() != null) {
            company = findCompany(request.companyId());
            job.setCompany(company);
            if (job.getCompanyLocation() != null
                    && !job.getCompanyLocation().getCompany().getId().equals(company.getId())) {
                job.setCompanyLocation(null);
            }
        }
        if (Boolean.TRUE.equals(request.clearCompanyLocation())) {
            job.setCompanyLocation(null);
        }
        if (request.companyLocationId() != null) {
            job.setCompanyLocation(resolveLocation(company, request.companyLocationId()));
        }
        if (request.title() != null) {
            String title = requireText(request.title(), "title");
            job.setTitle(title);
            job.setNormalizedTitle(TextNormalizer.normalizeName(title));
        }
        if (request.description() != null) {
            job.setDescription(trimToNull(request.description()));
        }
        if (request.employmentType() != null) {
            job.setEmploymentType(request.employmentType());
        }
        if (request.workMode() != null) {
            job.setWorkMode(request.workMode());
        }
        if (Boolean.TRUE.equals(request.clearExperience())) {
            job.setExperienceMinYears(null);
            job.setExperienceMaxYears(null);
        } else if (request.experienceMinYears() != null || request.experienceMaxYears() != null) {
            BigDecimal min = request.experienceMinYears() != null
                    ? request.experienceMinYears()
                    : job.getExperienceMinYears();
            BigDecimal max = request.experienceMaxYears() != null
                    ? request.experienceMaxYears()
                    : job.getExperienceMaxYears();
            applyExperience(job, min, max);
        }
        if (Boolean.TRUE.equals(request.clearSalary())) {
            clearSalary(job);
        } else if (request.salaryMin() != null || request.salaryMax() != null
                || request.salaryCurrency() != null || request.salaryPeriod() != null) {
            BigDecimal min = request.salaryMin() != null ? request.salaryMin() : job.getSalaryMin();
            BigDecimal max = request.salaryMax() != null ? request.salaryMax() : job.getSalaryMax();
            String currency = request.salaryCurrency() != null
                    ? request.salaryCurrency()
                    : job.getSalaryCurrency();
            String period = request.salaryPeriod() != null ? request.salaryPeriod() : job.getSalaryPeriod();
            if (min == null && max == null) {
                clearSalary(job);
            } else {
                applySalary(job, min, max, currency, period);
            }
        }
        if (Boolean.TRUE.equals(request.clearExpiresAt())) {
            job.setExpiresAt(null);
        } else if (request.expiresAt() != null) {
            job.setExpiresAt(request.expiresAt());
        }
        if (request.skills() != null) {
            replaceSkills(job, request.skills());
        }
        jobRepository.saveAndFlush(job);
        return toDetail(job);
    }

    @Transactional
    public AdminJobDetailResponse publish(UUID id) {
        Job job = findJob(id);
        Instant now = Instant.now();
        if (job.getStatus() != JobStatus.ACTIVE) {
            lifecycleService.transition(job, JobStatus.ACTIVE, "Published by administrator", null, now);
        }
        if (job.getFirstPublishedAt() == null) {
            job.setFirstPublishedAt(now);
        }
        job.setLastVerifiedAt(now);
        job.setStaleSince(null);
        job.setExpiredAt(null);
        jobRepository.saveAndFlush(job);
        return toDetail(job);
    }

    @Transactional
    public AdminJobDetailResponse expire(UUID id) {
        Job job = findJob(id);
        if (job.getStatus() == JobStatus.EXPIRED) {
            return toDetail(job);
        }
        if (job.getStatus() == JobStatus.REJECTED) {
            throw InvalidParameterException.of("status", "removed jobs cannot be expired");
        }
        Instant now = Instant.now();
        lifecycleService.transition(job, JobStatus.EXPIRED, "Expired by administrator", null, now);
        job.setExpiredAt(now);
        jobRepository.saveAndFlush(job);
        return toDetail(job);
    }

    @Transactional
    public AdminJobDetailResponse remove(UUID id) {
        Job job = findJob(id);
        if (job.getStatus() == JobStatus.REJECTED) {
            return toDetail(job);
        }
        Instant now = Instant.now();
        lifecycleService.transition(job, JobStatus.REJECTED, "Removed by administrator", null, now);
        for (JobSource source : jobSourceRepository.findByJob_IdOrderByFirstSeenAtAsc(id)) {
            if (!"REMOVED".equals(source.getSourceStatus())) {
                source.setSourceStatus("REMOVED");
                source.setRemovedAt(now);
                jobSourceRepository.save(source);
            }
        }
        jobRepository.saveAndFlush(job);
        return toDetail(job);
    }

    @Transactional
    public AdminJobDetailResponse markStale(UUID id) {
        Job job = findJob(id);
        if (job.getStatus() == JobStatus.STALE) {
            return toDetail(job);
        }
        if (job.getStatus() != JobStatus.ACTIVE) {
            throw InvalidParameterException.of("status", "only ACTIVE jobs can be marked stale");
        }
        Instant now = Instant.now();
        lifecycleService.transition(job, JobStatus.STALE, "Marked stale by administrator", null, now);
        job.setStaleSince(now);
        jobRepository.saveAndFlush(job);
        return toDetail(job);
    }

    @Transactional
    public AdminJobDetailResponse markUnverified(UUID id) {
        Job job = findJob(id);
        job.setLastVerifiedAt(null);
        jobRepository.saveAndFlush(job);
        return toDetail(job);
    }

    @Transactional
    public AdminJobSourceResponse addSource(UUID id, JobSourceInput request) {
        Job job = findJob(id);
        JobSource source = buildSource(job, request, Instant.now());
        source = jobSourceRepository.saveAndFlush(source);
        return AdminJobSourceResponse.from(source);
    }

    @Transactional
    public AdminJobSourceResponse updateSource(UUID id, UUID sourceId, UpdateJobSourceRequest request) {
        findJob(id);
        JobSource source = jobSourceRepository.findById(sourceId)
                .filter(found -> found.getJob().getId().equals(id))
                .orElseThrow(() -> new ResourceNotFoundException("Job source", sourceId));

        if (request.sourceName() != null) {
            source.setSourceName(requireText(request.sourceName(), "sourceName"));
        }
        if (request.sourceJobId() != null) {
            source.setSourceJobId(trimToNull(request.sourceJobId()));
        }
        if (request.sourceUrl() != null) {
            source.setSourceUrl(requireText(request.sourceUrl(), "sourceUrl"));
        }
        if (request.applicationUrl() != null) {
            source.setApplicationUrl(trimToNull(request.applicationUrl()));
        }
        if (request.sourceStatus() != null) {
            applySourceStatus(source, request.sourceStatus(), Instant.now());
        }
        validateSourceUrls(source);
        jobSourceRepository.saveAndFlush(source);
        return AdminJobSourceResponse.from(source);
    }

    @Transactional
    public AdminJobDetailResponse flagDuplicate(UUID id, FlagDuplicateRequest request) {
        Job job = findJob(id);
        Job duplicateOf = findJob(request.duplicateOfJobId());
        if (job.getId().equals(duplicateOf.getId())) {
            throw InvalidParameterException.of("duplicateOfJobId", "a job cannot be a duplicate of itself");
        }
        String reason = "Flagged as possible duplicate of job " + duplicateOf.getId()
                + noteSuffix(request.note());
        Instant now = Instant.now();
        if (job.getStatus() == JobStatus.NEEDS_REVIEW) {
            recordHistory(job, JobStatus.NEEDS_REVIEW, reason, now);
        } else {
            lifecycleService.transition(job, JobStatus.NEEDS_REVIEW, reason, null, now);
        }
        jobRepository.saveAndFlush(job);
        return toDetail(job);
    }

    @Transactional
    public AdminJobDetailResponse mergeJobs(UUID id, MergeJobsRequest request) {
        Job target = findJob(id);
        Job duplicate = findJob(request.duplicateJobId());
        if (target.getId().equals(duplicate.getId())) {
            throw InvalidParameterException.of("duplicateJobId", "a job cannot be merged into itself");
        }
        if (!target.getCompany().getId().equals(duplicate.getCompany().getId())) {
            throw InvalidParameterException.of("duplicateJobId",
                    "both jobs must belong to the same company");
        }
        if (duplicate.getStatus() == JobStatus.REJECTED) {
            throw InvalidParameterException.of("duplicateJobId",
                    "the duplicate job has already been removed or merged");
        }

        for (JobSource source : jobSourceRepository.findByJob_IdOrderByFirstSeenAtAsc(duplicate.getId())) {
            if (source.getSourceJobId() != null) {
                boolean sourceAlreadyOnTarget = jobSourceRepository
                        .findFirstBySourceNameAndSourceJobId(source.getSourceName(), source.getSourceJobId())
                        .filter(existing -> existing.getJob().getId().equals(target.getId()))
                        .isPresent();
                if (sourceAlreadyOnTarget) {
                    jobSourceRepository.delete(source);
                    continue;
                }
            }
            source.setJob(target);
            jobSourceRepository.save(source);
        }

        Map<UUID, JobSkill> targetSkills = new LinkedHashMap<>();
        for (JobSkill jobSkill : jobSkillRepository.findByJob_Id(target.getId())) {
            targetSkills.put(jobSkill.getSkill().getId(), jobSkill);
        }
        for (JobSkill duplicateSkill : jobSkillRepository.findByJob_Id(duplicate.getId())) {
            if (!targetSkills.containsKey(duplicateSkill.getSkill().getId())) {
                JobSkill jobSkill = new JobSkill();
                jobSkill.setId(new JobSkillId(target.getId(), duplicateSkill.getSkill().getId()));
                jobSkill.setJob(target);
                jobSkill.setSkill(duplicateSkill.getSkill());
                jobSkill.setRequirementType(duplicateSkill.getRequirementType());
                jobSkillRepository.save(jobSkill);
            }
            jobSkillRepository.delete(duplicateSkill);
        }

        Instant now = Instant.now();
        String reason = "Merged into job " + target.getId() + noteSuffix(request.note());
        lifecycleService.transition(duplicate, JobStatus.REJECTED, reason, null, now);
        jobRepository.saveAndFlush(duplicate);
        return toDetail(target);
    }

    private void addUrlMatches(String url, UUID excludeId, Map<UUID, Job> matches) {
        for (JobSource source : jobSourceRepository.findByCanonicalUrl(url)) {
            Job job = source.getJob();
            if (!job.getId().equals(excludeId)) {
                matches.put(job.getId(), job);
            }
        }
    }

    private Job findJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }

    private Company findCompany(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private CompanyLocation resolveLocation(Company company, UUID locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findById(locationId)
                .filter(location -> location.getCompany().getId().equals(company.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Company location", locationId));
    }

    private void applyExperience(Job job, BigDecimal min, BigDecimal max) {
        validateRange(min, max, "experience");
        job.setExperienceMinYears(min);
        job.setExperienceMaxYears(max);
    }

    private void applySalary(Job job, BigDecimal min, BigDecimal max, String currency, String period) {
        if (min == null && max == null) {
            clearSalary(job);
            return;
        }
        if (min == null || max == null) {
            throw InvalidParameterException.of("salaryMin",
                    "salary requires both minimum and maximum bounds");
        }
        validateRange(min, max, "salary");
        String normalizedCurrency = trimToNull(currency);
        if (normalizedCurrency == null || !normalizedCurrency.matches("[A-Za-z]{3}")) {
            throw InvalidParameterException.of("salaryCurrency",
                    "a three-letter currency code is required when salary is present");
        }
        String normalizedPeriod = trimToNull(period);
        if (normalizedPeriod == null) {
            throw InvalidParameterException.of("salaryPeriod",
                    "is required when salary is present");
        }
        job.setSalaryMin(min);
        job.setSalaryMax(max);
        job.setSalaryCurrency(normalizedCurrency.toUpperCase(Locale.ROOT));
        job.setSalaryPeriod(normalizedPeriod.toUpperCase(Locale.ROOT));
    }

    private void clearSalary(Job job) {
        job.setSalaryMin(null);
        job.setSalaryMax(null);
        job.setSalaryCurrency(null);
        job.setSalaryPeriod(null);
    }

    private void validateRange(BigDecimal min, BigDecimal max, String field) {
        if (min != null && min.signum() < 0) {
            throw InvalidParameterException.of(field + "Min", "must be non-negative");
        }
        if (max != null && max.signum() < 0) {
            throw InvalidParameterException.of(field + "Max", "must be non-negative");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw InvalidParameterException.of(field + "Min",
                    "must be less than or equal to " + field + "Max");
        }
    }

    private void replaceSkills(Job job, List<JobSkillInput> inputs) {
        Map<UUID, JobSkill> existing = new LinkedHashMap<>();
        for (JobSkill jobSkill : jobSkillRepository.findByJob_Id(job.getId())) {
            existing.put(jobSkill.getSkill().getId(), jobSkill);
        }
        Map<UUID, ResolvedSkill> desired = resolveSkills(inputs);
        for (ResolvedSkill resolved : desired.values()) {
            JobSkill current = existing.remove(resolved.skill().getId());
            if (current != null) {
                if (current.getRequirementType() != resolved.requirementType()) {
                    current.setRequirementType(resolved.requirementType());
                    jobSkillRepository.save(current);
                }
            } else {
                JobSkill jobSkill = new JobSkill();
                jobSkill.setId(new JobSkillId(job.getId(), resolved.skill().getId()));
                jobSkill.setJob(job);
                jobSkill.setSkill(resolved.skill());
                jobSkill.setRequirementType(resolved.requirementType());
                jobSkillRepository.save(jobSkill);
            }
        }
        for (JobSkill leftover : existing.values()) {
            jobSkillRepository.delete(leftover);
        }
    }

    private Map<UUID, ResolvedSkill> resolveSkills(List<JobSkillInput> inputs) {
        Map<UUID, ResolvedSkill> resolved = new LinkedHashMap<>();
        for (JobSkillInput input : inputs) {
            String name = requireText(input.name(), "skills.name");
            Skill skill = skillRepository.findByNormalizedName(TextNormalizer.normalizeName(name))
                    .orElseThrow(() -> new ResourceNotFoundException("Skill", name));
            SkillRequirementType requirementType = input.requirementType() == null
                    ? SkillRequirementType.REQUIRED
                    : input.requirementType();
            resolved.putIfAbsent(skill.getId(), new ResolvedSkill(skill, requirementType));
        }
        return resolved;
    }

    private JobSource buildSource(Job job, JobSourceInput input, Instant now) {
        JobSource source = new JobSource();
        source.setId(UUID.randomUUID());
        source.setJob(job);
        source.setSourceName(requireText(input.sourceName(), "sourceName"));
        source.setSourceJobId(trimToNull(input.sourceJobId()));
        source.setSourceUrl(requireText(input.sourceUrl(), "sourceUrl"));
        source.setApplicationUrl(trimToNull(input.applicationUrl()));
        source.setSourceStatus("ACTIVE");
        source.setFirstSeenAt(now);
        source.setLastSeenAt(now);
        source.setLastCheckedAt(now);
        source.setConsecutiveMissingChecks(0);
        validateSourceUrls(source);
        return source;
    }

    private void validateSourceUrls(JobSource source) {
        validateAbsoluteUrl(source.getSourceUrl(), "sourceUrl", true);
        validateAbsoluteUrl(source.getApplicationUrl(), "applicationUrl", false);
    }

    private void validateAbsoluteUrl(String value, String field, boolean required) {
        if (value == null) {
            if (required) {
                throw InvalidParameterException.of(field, "must not be blank");
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
            throw InvalidParameterException.of(field, "must be an absolute http(s) URL");
        }
    }

    private void applySourceStatus(JobSource source, String status, Instant now) {
        source.setSourceStatus(status);
        if ("REMOVED".equals(status)) {
            if (source.getRemovedAt() == null) {
                source.setRemovedAt(now);
            }
        } else {
            source.setRemovedAt(null);
        }
        if ("ACTIVE".equals(status)) {
            source.setConsecutiveMissingChecks(0);
        }
    }

    private void recordHistory(Job job, JobStatus newStatus, String reason, Instant now) {
        JobStatusHistory history = new JobStatusHistory();
        history.setId(UUID.randomUUID());
        history.setJob(job);
        history.setOldStatus(job.getStatus());
        history.setNewStatus(newStatus);
        history.setReason(reason);
        history.setChangedAt(now);
        statusHistoryRepository.save(history);
    }

    private AdminJobSummaryResponse toSummary(Job job) {
        return new AdminJobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getStatus() == null ? null : job.getStatus().name(),
                job.getEmploymentType() == null ? null : job.getEmploymentType().name(),
                job.getWorkMode() == null ? null : job.getWorkMode().name(),
                job.getExperienceMinYears(),
                job.getExperienceMaxYears(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getSalaryPeriod(),
                postedAt(job),
                job.getLastVerifiedAt(),
                job.getFirstSeenAt(),
                job.getLastSeenAt(),
                job.getLastCheckedAt(),
                job.getExpiresAt(),
                job.getCompany() == null ? null : JobCompanyResponse.from(job.getCompany()),
                job.getCompanyLocation() == null ? null : CompanyLocationResponse.from(job.getCompanyLocation()),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    private AdminJobDetailResponse toDetail(Job job) {
        List<JobSkillResponse> skills = jobSkillRepository.findByJob_Id(job.getId()).stream()
                .sorted(Comparator.comparing(skill -> skill.getSkill().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(JobSkillResponse::from)
                .toList();
        List<AdminJobSourceResponse> sources = jobSourceRepository.findByJob_IdOrderByFirstSeenAtAsc(job.getId())
                .stream()
                .map(AdminJobSourceResponse::from)
                .toList();
        List<JobStatusHistoryResponse> history = statusHistoryRepository
                .findByJob_IdOrderByChangedAtAsc(job.getId()).stream()
                .map(JobStatusHistoryResponse::from)
                .toList();
        return new AdminJobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getStatus() == null ? null : job.getStatus().name(),
                job.getEmploymentType() == null ? null : job.getEmploymentType().name(),
                job.getWorkMode() == null ? null : job.getWorkMode().name(),
                job.getExperienceMinYears(),
                job.getExperienceMaxYears(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getSalaryPeriod(),
                postedAt(job),
                job.getFirstPublishedAt(),
                job.getLastVerifiedAt(),
                job.getFirstSeenAt(),
                job.getLastSeenAt(),
                job.getLastCheckedAt(),
                job.getStaleSince(),
                job.getExpiresAt(),
                job.getExpiredAt(),
                job.getCompany() == null ? null : JobCompanyResponse.from(job.getCompany()),
                job.getCompanyLocation() == null ? null : CompanyLocationResponse.from(job.getCompanyLocation()),
                skills,
                sources,
                history,
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    private Specification<Job> toSpecification(String keyword, UUID companyId, JobStatus status,
                                               EmploymentType employmentType, WorkMode workMode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalizedKeyword = TextNormalizer.normalizeName(keyword);
            if (normalizedKeyword != null && !normalizedKeyword.isEmpty()) {
                Join<Job, Company> company = root.join("company", JoinType.INNER);
                String pattern = TextNormalizer.likePattern(normalizedKeyword);
                predicates.add(cb.or(
                        cb.like(root.get("normalizedTitle"), pattern, '\\'),
                        cb.like(company.get("normalizedName"), pattern, '\\')));
            }
            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (employmentType != null) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType));
            }
            if (workMode != null) {
                predicates.add(cb.equal(root.get("workMode"), workMode));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort sort(String sort) {
        return switch (sort == null ? "updated" : sort) {
            case "updated" -> Sort.by(Sort.Order.desc("updatedAt"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"));
            case "title" -> Sort.by(Sort.Order.asc("normalizedTitle"));
            case "posted" -> Sort.by(Sort.Order.desc("firstPublishedAt").nullsLast());
            default -> throw InvalidParameterException.of(
                    "sort", "allowed values are 'updated', 'newest', 'title' and 'posted'");
        };
    }

    private Instant postedAt(Job job) {
        return job.getFirstPublishedAt() != null ? job.getFirstPublishedAt() : job.getCreatedAt();
    }

    private String noteSuffix(String note) {
        String trimmed = trimToNull(note);
        return trimmed == null ? "" : " (" + trimmed + ")";
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw InvalidParameterException.of(field, "must not be blank");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ResolvedSkill(Skill skill, SkillRequirementType requirementType) {
    }
}
