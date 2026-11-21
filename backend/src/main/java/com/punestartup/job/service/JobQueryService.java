package com.punestartup.job.service;

import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.util.TextNormalizer;
import com.punestartup.common.web.PageRequests;
import com.punestartup.company.dto.CompanyLocationResponse;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.entity.Industry;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.job.dto.JobCompanyResponse;
import com.punestartup.job.dto.JobDetailResponse;
import com.punestartup.job.dto.JobSearchRequest;
import com.punestartup.job.dto.JobSkillResponse;
import com.punestartup.job.dto.JobSourceResponse;
import com.punestartup.job.dto.JobSummaryResponse;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobSkill;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.repository.JobRepository;
import com.punestartup.job.repository.JobSourceRepository;
import com.punestartup.skill.entity.Skill;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueryService {

    private final JobRepository jobRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobSummaryMapper summaryMapper;
    private final int maxPageSize;
    private final BigDecimal fresherMaxExperienceYears;
    private final int maxAgeDays;

    public JobQueryService(JobRepository jobRepository,
                           JobSourceRepository jobSourceRepository,
                           JobSummaryMapper summaryMapper,
                           @Value("${app.search.max-page-size}") int maxPageSize,
                           @Value("${app.jobs.fresher-max-experience-years}") BigDecimal fresherMaxExperienceYears,
                           @Value("${app.jobs.max-age-days}") int maxAgeDays) {
        this.jobRepository = jobRepository;
        this.jobSourceRepository = jobSourceRepository;
        this.summaryMapper = summaryMapper;
        this.maxPageSize = maxPageSize;
        this.fresherMaxExperienceYears = fresherMaxExperienceYears;
        this.maxAgeDays = maxAgeDays;
    }

    @Transactional(readOnly = true)
    public Page<JobSummaryResponse> searchJobs(JobSearchRequest request) {
        PageRequest pageRequest = buildPageRequest(request);
        return jobRepository.findAll(toSpecification(request), pageRequest).map(summaryMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public JobDetailResponse getJobDetail(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .filter(found -> found.getStatus() == JobStatus.ACTIVE)
                .filter(this::isFresh)
                .filter(found -> found.getCompany() != null
                        && found.getCompany().getRecordStatus() == RecordStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        List<JobSkillResponse> skills = job.getSkills().stream()
                .sorted(Comparator.comparing(skill -> skill.getSkill().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(JobSkillResponse::from)
                .toList();
        List<JobSourceResponse> sources = jobSourceRepository.findByJob_IdOrderByFirstSeenAtAsc(jobId).stream()
                .map(JobSourceResponse::from)
                .toList();
        CompanyLocation location = job.getCompanyLocation();

        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getEmploymentType() == null ? null : job.getEmploymentType().name(),
                job.getWorkMode() == null ? null : job.getWorkMode().name(),
                job.getExperienceMinYears(),
                job.getExperienceMaxYears(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryCurrency(),
                job.getSalaryPeriod(),
                job.getStatus() == null ? null : job.getStatus().name(),
                summaryMapper.postedAt(job),
                job.getFirstPublishedAt(),
                job.getLastVerifiedAt(),
                job.getFirstSeenAt(),
                job.getLastSeenAt(),
                job.getLastCheckedAt(),
                job.getExpiresAt(),
                job.isFresherEligible(fresherMaxExperienceYears),
                JobCompanyResponse.from(job.getCompany()),
                location == null ? null : CompanyLocationResponse.from(location),
                skills,
                sources,
                job.getCreatedAt(),
                job.getUpdatedAt());
    }

    private PageRequest buildPageRequest(JobSearchRequest request) {
        switch (request.sort()) {
            case "newest", "oldest", "salary" -> {
            }
            default -> throw InvalidParameterException.of("sort",
                    "allowed values are 'newest', 'oldest' and 'salary'");
        }
        return PageRequests.of(request.page(), request.size(), null, maxPageSize);
    }

    private Specification<Job> toSpecification(JobSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), JobStatus.ACTIVE));
            Expression<Instant> postedAt = cb.coalesce(root.get("firstPublishedAt"), root.get("createdAt"));
            predicates.add(cb.greaterThanOrEqualTo(postedAt,
                    Instant.now().minus(Duration.ofDays(maxAgeDays))));

            Join<Job, Company> company = root.join("company", JoinType.INNER);
            predicates.add(cb.equal(company.get("recordStatus"), RecordStatus.ACTIVE));

            String keyword = TextNormalizer.normalizeName(request.keyword());
            if (keyword != null && !keyword.isEmpty()) {
                String pattern = TextNormalizer.likePattern(keyword);
                Predicate titleMatch = cb.like(root.get("normalizedTitle"), pattern, '\\');
                Predicate companyMatch = cb.like(company.get("normalizedName"), pattern, '\\');

                Subquery<UUID> skillMatch = query.subquery(UUID.class);
                Root<JobSkill> jobSkill = skillMatch.from(JobSkill.class);
                Join<JobSkill, Skill> skill = jobSkill.join("skill", JoinType.INNER);
                skillMatch.select(jobSkill.get("job").get("id"))
                        .where(cb.like(skill.get("normalizedName"), pattern, '\\'));

                predicates.add(cb.or(titleMatch, companyMatch, root.get("id").in(skillMatch)));
            }

            if (request.companyId() != null) {
                predicates.add(cb.equal(company.get("id"), request.companyId()));
            }

            String city = TextNormalizer.normalizeName(request.city());
            String area = TextNormalizer.normalizeName(request.area());
            if ((city != null && !city.isEmpty()) || (area != null && !area.isEmpty())) {
                Join<Job, CompanyLocation> location = root.join("companyLocation", JoinType.INNER);
                if (city != null && !city.isEmpty()) {
                    predicates.add(cb.like(cb.lower(location.get("city")),
                            TextNormalizer.likePattern(city), '\\'));
                }
                if (area != null && !area.isEmpty()) {
                    predicates.add(cb.like(cb.lower(location.get("area")),
                            TextNormalizer.likePattern(area), '\\'));
                }
            }

            String industry = TextNormalizer.normalizeName(request.industry());
            if (industry != null && !industry.isEmpty()) {
                Join<Company, Industry> industryJoin = company.join("industry", JoinType.INNER);
                predicates.add(cb.or(
                        cb.equal(cb.lower(industryJoin.get("name")), industry),
                        cb.equal(cb.lower(industryJoin.get("slug")), industry)));
            }

            if (request.employmentType() != null) {
                predicates.add(cb.equal(root.get("employmentType"), request.employmentType()));
            }
            if (request.workMode() != null) {
                predicates.add(cb.equal(root.get("workMode"), request.workMode()));
            }
            if (request.fresherOnly()) {
                predicates.add(cb.or(
                        cb.isNull(root.get("experienceMinYears")),
                        cb.le(root.get("experienceMinYears"), fresherMaxExperienceYears)));
            }
            if (request.experienceYears() != null) {
                BigDecimal years = request.experienceYears();
                predicates.add(cb.or(
                        cb.isNull(root.get("experienceMinYears")),
                        cb.le(root.get("experienceMinYears"), years)));
                predicates.add(cb.or(
                        cb.isNull(root.get("experienceMaxYears")),
                        cb.ge(root.get("experienceMaxYears"), years)));
            }

            for (String requestedSkill : request.skills()) {
                String normalizedSkill = TextNormalizer.normalizeName(requestedSkill);
                if (normalizedSkill == null || normalizedSkill.isEmpty()) {
                    continue;
                }
                Subquery<UUID> skillMatch = query.subquery(UUID.class);
                Root<JobSkill> jobSkill = skillMatch.from(JobSkill.class);
                Join<JobSkill, Skill> skill = jobSkill.join("skill", JoinType.INNER);
                skillMatch.select(jobSkill.get("job").get("id"))
                        .where(cb.equal(skill.get("normalizedName"), normalizedSkill));
                predicates.add(root.get("id").in(skillMatch));
            }

            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                switch (request.sort()) {
                    case "newest" -> query.orderBy(cb.desc(postedAt));
                    case "oldest" -> query.orderBy(cb.asc(postedAt));
                    case "salary" -> query.orderBy(cb.desc(
                            cb.coalesce(root.get("salaryMax"), BigDecimal.valueOf(-1))));
                    default -> {
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean isFresh(Job job) {
        Instant postedAt = job.getFirstPublishedAt() != null ? job.getFirstPublishedAt() : job.getCreatedAt();
        return postedAt != null && !postedAt.isBefore(Instant.now().minus(Duration.ofDays(maxAgeDays)));
    }

}
