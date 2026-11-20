package com.punestartup.company.service;

import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.util.TextNormalizer;
import com.punestartup.company.dto.CompanyDetailResponse;
import com.punestartup.company.dto.CompanySearchRequest;
import com.punestartup.company.dto.CompanySourceResponse;
import com.punestartup.company.dto.CompanySummaryResponse;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.company.repository.CompanySourceRepository;
import com.punestartup.job.entity.Job;
import com.punestartup.job.entity.JobStatus;
import com.punestartup.job.repository.CompanyJobCountProjection;
import com.punestartup.job.repository.JobRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyQueryService {

    private final CompanyRepository companyRepository;
    private final CompanyLocationRepository locationRepository;
    private final CompanySourceRepository sourceRepository;
    private final JobRepository jobRepository;
    private final int maxPageSize;
    private final BigDecimal fresherMaxExperienceYears;

    public CompanyQueryService(CompanyRepository companyRepository,
                               CompanyLocationRepository locationRepository,
                               CompanySourceRepository sourceRepository,
                               JobRepository jobRepository,
                               @Value("${app.search.max-page-size}") int maxPageSize,
                               @Value("${app.jobs.fresher-max-experience-years}") BigDecimal fresherMaxExperienceYears) {
        this.companyRepository = companyRepository;
        this.locationRepository = locationRepository;
        this.sourceRepository = sourceRepository;
        this.jobRepository = jobRepository;
        this.maxPageSize = maxPageSize;
        this.fresherMaxExperienceYears = fresherMaxExperienceYears;
    }

    @Transactional(readOnly = true)
    public Page<CompanySummaryResponse> searchCompanies(CompanySearchRequest request) {
        PageRequest pageRequest = buildPageRequest(request);
        Page<Company> companies = companyRepository.findAll(toSpecification(request), pageRequest);
        if (companies.isEmpty()) {
            return Page.empty(pageRequest);
        }

        Set<UUID> companyIds = companies.getContent().stream().map(Company::getId).collect(Collectors.toSet());
        Map<UUID, Long> activeJobCounts = toMap(jobRepository.countActiveByCompanyIds(companyIds));
        Map<UUID, Long> fresherJobCounts =
                toMap(jobRepository.countFresherEligibleByCompanyIds(companyIds, fresherMaxExperienceYears));
        Map<UUID, List<CompanyLocation>> locationsByCompany =
                locationRepository.findByCompanyIdIn(companyIds).stream()
                        .collect(Collectors.groupingBy(location -> location.getCompany().getId()));

        return companies.map(company -> {
            List<CompanyLocation> locations = locationsByCompany.getOrDefault(company.getId(), List.of());
            Set<String> areas = locations.stream()
                    .map(CompanyLocation::getArea)
                    .filter(area -> area != null && !area.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return new CompanySummaryResponse(
                    company.getId(),
                    company.getName(),
                    company.getSlug(),
                    company.getIndustry() == null ? null : company.getIndustry().getName(),
                    company.getWebsiteUrl(),
                    company.getLogoUrl(),
                    company.getFundingStage(),
                    company.getEmployeeCountMin(),
                    company.getEmployeeCountMax(),
                    company.getVerificationStatus() == null ? null : company.getVerificationStatus().name(),
                    List.copyOf(areas),
                    activeJobCounts.getOrDefault(company.getId(), 0L),
                    fresherJobCounts.getOrDefault(company.getId(), 0L));
        });
    }

    @Transactional(readOnly = true)
    public List<String> listAreas() {
        return locationRepository.findDistinctAreasByCompanyRecordStatus(RecordStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public CompanyDetailResponse getCompanyDetail(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .filter(found -> found.getRecordStatus() == RecordStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));

        List<CompanyLocation> locations = locationRepository.findByCompanyIdOrderByHeadquartersDesc(companyId);
        List<CompanySourceResponse> sources = sourceRepository.findByCompanyIdOrderByCreatedAtAsc(companyId).stream()
                .map(source -> new CompanySourceResponse(
                        source.getSourceName(),
                        source.getSourceUrl(),
                        source.getExternalId(),
                        source.getLastVerifiedAt()))
                .toList();

        long activeJobCount = toMap(jobRepository.countActiveByCompanyIds(List.of(companyId)))
                .getOrDefault(companyId, 0L);
        long fresherJobCount = toMap(jobRepository.countFresherEligibleByCompanyIds(List.of(companyId),
                fresherMaxExperienceYears)).getOrDefault(companyId, 0L);

        return new CompanyDetailResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getWebsiteUrl(),
                company.getLogoUrl(),
                company.getDescription(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getIndustry() == null ? null : company.getIndustry().getSlug(),
                company.getFoundedYear(),
                company.getEmployeeCountMin(),
                company.getEmployeeCountMax(),
                company.getFundingStage(),
                company.getCompanyType() == null ? null : company.getCompanyType().name(),
                company.getVerificationStatus() == null ? null : company.getVerificationStatus().name(),
                locations.stream().map(com.punestartup.company.dto.CompanyLocationResponse::from).toList(),
                sources,
                activeJobCount,
                fresherJobCount,
                company.getCreatedAt(),
                company.getUpdatedAt());
    }

    private PageRequest buildPageRequest(CompanySearchRequest request) {
        if (request.page() < 0) {
            throw InvalidParameterException.of("page", "must be 0 or greater");
        }
        if (request.size() < 1 || request.size() > maxPageSize) {
            throw InvalidParameterException.of("size", "must be between 1 and %d".formatted(maxPageSize));
        }
        Sort sort = switch (request.sort()) {
            case "name" -> Sort.by(Sort.Direction.ASC, "normalizedName");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "updated" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> throw InvalidParameterException.of(
                    "sort", "allowed values are 'name', 'newest' and 'updated'");
        };
        return PageRequest.of(request.page(), request.size(), sort);
    }

    private Specification<Company> toSpecification(CompanySearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("recordStatus"), RecordStatus.ACTIVE));

            String keyword = TextNormalizer.normalizeName(request.keyword());
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.like(root.get("normalizedName"), TextNormalizer.likePattern(keyword), '\\'));
            }

            String city = TextNormalizer.normalizeName(request.city());
            String area = TextNormalizer.normalizeName(request.area());
            if (city != null || area != null) {
                Join<Company, CompanyLocation> locations = root.join("locations", JoinType.INNER);
                if (city != null && !city.isEmpty()) {
                    predicates.add(cb.like(cb.lower(locations.get("city")),
                            TextNormalizer.likePattern(city), '\\'));
                }
                if (area != null && !area.isEmpty()) {
                    predicates.add(cb.like(cb.lower(locations.get("area")),
                            TextNormalizer.likePattern(area), '\\'));
                }
                query.distinct(true);
            }

            String industry = TextNormalizer.normalizeName(request.industry());
            if (industry != null && !industry.isEmpty()) {
                Join<Company, com.punestartup.company.entity.Industry> industryJoin =
                        root.join("industry", JoinType.INNER);
                predicates.add(cb.or(
                        cb.equal(cb.lower(industryJoin.get("name")), industry),
                        cb.equal(cb.lower(industryJoin.get("slug")), industry)));
            }

            if (request.fresherOnly()) {
                Subquery<UUID> activeFresherJobs = query.subquery(UUID.class);
                Root<Job> job = activeFresherJobs.from(Job.class);
                activeFresherJobs.select(job.get("company").get("id"))
                        .where(
                                cb.equal(job.get("status"), JobStatus.ACTIVE),
                                cb.or(
                                        cb.isNull(job.get("experienceMinYears")),
                                        cb.le(job.get("experienceMinYears"), fresherMaxExperienceYears)));
                predicates.add(root.get("id").in(activeFresherJobs));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<UUID, Long> toMap(List<CompanyJobCountProjection> counts) {
        return counts.stream().collect(Collectors.toMap(CompanyJobCountProjection::getCompanyId,
                CompanyJobCountProjection::getJobCount));
    }
}
