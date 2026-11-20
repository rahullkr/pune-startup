package com.punestartup.company.service;

import com.punestartup.common.exception.ConflictException;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.util.TextNormalizer;
import com.punestartup.common.web.PageRequests;
import com.punestartup.company.dto.AdminCompanyResponse;
import com.punestartup.company.dto.AdminCompanySourceResponse;
import com.punestartup.company.dto.AdminCompanySummaryResponse;
import com.punestartup.company.dto.CompanyLocationResponse;
import com.punestartup.company.dto.CreateCompanyLocationRequest;
import com.punestartup.company.dto.CreateCompanyRequest;
import com.punestartup.company.dto.UpdateCompanyLocationRequest;
import com.punestartup.company.dto.UpdateCompanyRequest;
import com.punestartup.company.entity.Company;
import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.GeocodeStatus;
import com.punestartup.company.entity.Industry;
import com.punestartup.company.entity.LocationType;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import com.punestartup.company.repository.CompanyLocationRepository;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.company.repository.CompanySourceRepository;
import com.punestartup.company.repository.IndustryRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.net.URI;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCompanyService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final CompanyRepository companyRepository;
    private final CompanyLocationRepository locationRepository;
    private final CompanySourceRepository sourceRepository;
    private final IndustryRepository industryRepository;
    private final int maxPageSize;

    public AdminCompanyService(CompanyRepository companyRepository,
                               CompanyLocationRepository locationRepository,
                               CompanySourceRepository sourceRepository,
                               IndustryRepository industryRepository,
                               @Value("${app.search.max-page-size}") int maxPageSize) {
        this.companyRepository = companyRepository;
        this.locationRepository = locationRepository;
        this.sourceRepository = sourceRepository;
        this.industryRepository = industryRepository;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<AdminCompanySummaryResponse> listCompanies(String keyword, RecordStatus recordStatus,
                                                           VerificationStatus verificationStatus, String industry,
                                                           String sort, int page, int size) {
        PageRequest pageRequest = PageRequests.of(page, size, sort(sort), maxPageSize);
        return companyRepository.findAll(toSpecification(keyword, recordStatus, verificationStatus, industry),
                        pageRequest)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public AdminCompanyResponse getCompany(UUID id) {
        return toResponse(findCompany(id));
    }

    @Transactional(readOnly = true)
    public List<AdminCompanySummaryResponse> findDuplicates(String name, String websiteUrl, UUID excludeId) {
        String normalizedName = TextNormalizer.normalizeName(name);
        String websiteDomain = websiteDomain(trimToNull(websiteUrl));
        if ((normalizedName == null || normalizedName.isEmpty()) && websiteDomain == null) {
            throw InvalidParameterException.of("name", "provide a company name or websiteUrl to check duplicates");
        }

        Map<UUID, Company> matches = new LinkedHashMap<>();
        if (normalizedName != null && !normalizedName.isEmpty()) {
            companyRepository.findFirstByNormalizedName(normalizedName)
                    .filter(company -> !company.getId().equals(excludeId))
                    .ifPresent(company -> matches.put(company.getId(), company));
        }
        if (websiteDomain != null) {
            companyRepository.findFirstByWebsiteDomainIgnoreCase(websiteDomain)
                    .filter(company -> !company.getId().equals(excludeId))
                    .ifPresent(company -> matches.put(company.getId(), company));
        }

        return matches.values().stream()
                .limit(20)
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public AdminCompanyResponse createCompany(CreateCompanyRequest request) {
        String name = requireText(request.name(), "name");
        String normalizedName = TextNormalizer.normalizeName(name);
        ensureNameAvailable(normalizedName, null);

        String websiteUrl = trimToNull(request.websiteUrl());
        String websiteDomain = websiteDomain(websiteUrl);
        ensureDomainAvailable(websiteDomain, null);
        validateFoundedYear(request.foundedYear());
        validateEmployeeRange(request.employeeCountMin(), request.employeeCountMax());

        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName(name);
        company.setNormalizedName(normalizedName);
        company.setSlug(resolveSlug(request.slug(), name, null));
        company.setWebsiteUrl(websiteUrl);
        company.setWebsiteDomain(websiteDomain);
        company.setLogoUrl(trimToNull(request.logoUrl()));
        company.setDescription(trimToNull(request.description()));
        company.setIndustry(resolveIndustry(request.industrySlug()));
        company.setFoundedYear(request.foundedYear());
        company.setEmployeeCountMin(request.employeeCountMin());
        company.setEmployeeCountMax(request.employeeCountMax());
        company.setFundingStage(trimToNull(request.fundingStage()));
        company.setCompanyType(request.companyType() == null ? CompanyType.STARTUP : request.companyType());
        company.setVerificationStatus(request.verificationStatus() == null
                ? VerificationStatus.UNVERIFIED
                : request.verificationStatus());
        company.setRecordStatus(RecordStatus.INACTIVE);

        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public AdminCompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        Company company = findCompany(id);
        requireNotMerged(company);

        if (request.name() != null) {
            String name = requireText(request.name(), "name");
            String normalizedName = TextNormalizer.normalizeName(name);
            ensureNameAvailable(normalizedName, id);
            company.setName(name);
            company.setNormalizedName(normalizedName);
        }
        if (request.slug() != null) {
            String slug = TextNormalizer.slugify(request.slug());
            if (slug == null) {
                throw InvalidParameterException.of("slug", "must contain at least one letter or number");
            }
            ensureSlugAvailable(slug, id);
            company.setSlug(slug);
        }
        if (request.websiteUrl() != null) {
            String websiteUrl = trimToNull(request.websiteUrl());
            String websiteDomain = websiteDomain(websiteUrl);
            ensureDomainAvailable(websiteDomain, id);
            company.setWebsiteUrl(websiteUrl);
            company.setWebsiteDomain(websiteDomain);
        }
        if (request.logoUrl() != null) {
            company.setLogoUrl(trimToNull(request.logoUrl()));
        }
        if (request.description() != null) {
            company.setDescription(trimToNull(request.description()));
        }
        if (request.industrySlug() != null) {
            company.setIndustry(resolveIndustry(request.industrySlug()));
        }
        if (request.foundedYear() != null) {
            validateFoundedYear(request.foundedYear());
            company.setFoundedYear(request.foundedYear());
        }
        if (request.employeeCountMin() != null) {
            company.setEmployeeCountMin(request.employeeCountMin());
        }
        if (request.employeeCountMax() != null) {
            company.setEmployeeCountMax(request.employeeCountMax());
        }
        validateEmployeeRange(company.getEmployeeCountMin(), company.getEmployeeCountMax());
        if (request.fundingStage() != null) {
            company.setFundingStage(trimToNull(request.fundingStage()));
        }
        if (request.companyType() != null) {
            company.setCompanyType(request.companyType());
        }
        if (request.verificationStatus() != null) {
            company.setVerificationStatus(request.verificationStatus());
        }

        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public AdminCompanyResponse publishCompany(UUID id) {
        return changeStatus(id, RecordStatus.ACTIVE);
    }

    @Transactional
    public AdminCompanyResponse unpublishCompany(UUID id) {
        return changeStatus(id, RecordStatus.INACTIVE);
    }

    @Transactional
    public AdminCompanyResponse archiveCompany(UUID id) {
        return changeStatus(id, RecordStatus.ARCHIVED);
    }

    @Transactional
    public CompanyLocationResponse addLocation(UUID companyId, CreateCompanyLocationRequest request) {
        Company company = findCompany(companyId);
        requireNotMerged(company);

        GeocodeStatus geocodeStatus = request.geocodeStatus() == null
                ? GeocodeStatus.PENDING
                : request.geocodeStatus();
        Point location = buildPoint(request.latitude(), request.longitude(), geocodeStatus);

        if (Boolean.TRUE.equals(request.headquarters())) {
            clearHeadquarters(companyId, null);
        }

        CompanyLocation entity = new CompanyLocation();
        entity.setId(UUID.randomUUID());
        entity.setCompany(company);
        applyLocationFields(entity, request.locationName(), request.addressLine(), request.area(), request.city(),
                request.state(), request.countryCode(), request.postalCode(), request.locationType());
        entity.setLocation(location);
        entity.setGeocodeStatus(geocodeStatus);
        entity.setHeadquarters(Boolean.TRUE.equals(request.headquarters()));

        return CompanyLocationResponse.from(locationRepository.save(entity));
    }

    @Transactional
    public CompanyLocationResponse updateLocation(UUID companyId, UUID locationId,
                                                  UpdateCompanyLocationRequest request) {
        Company company = findCompany(companyId);
        requireNotMerged(company);
        CompanyLocation location = locationRepository.findById(locationId)
                .filter(found -> found.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Company location", locationId));

        if (request.locationName() != null) {
            location.setLocationName(trimToNull(request.locationName()));
        }
        if (request.addressLine() != null) {
            location.setAddressLine(trimToNull(request.addressLine()));
        }
        if (request.area() != null) {
            location.setArea(trimToNull(request.area()));
        }
        if (request.city() != null) {
            location.setCity(requireText(request.city(), "city"));
        }
        if (request.state() != null) {
            location.setState(requireText(request.state(), "state"));
        }
        if (request.countryCode() != null) {
            location.setCountryCode(request.countryCode().trim().toUpperCase(Locale.ROOT));
        }
        if (request.postalCode() != null) {
            location.setPostalCode(trimToNull(request.postalCode()));
        }
        if (request.locationType() != null) {
            location.setLocationType(request.locationType());
        }
        if (request.latitude() != null || request.longitude() != null) {
            GeocodeStatus status = request.geocodeStatus() == null
                    ? location.getGeocodeStatus()
                    : request.geocodeStatus();
            location.setLocation(buildPoint(request.latitude(), request.longitude(), status));
        }
        if (request.geocodeStatus() != null) {
            validateGeocodeStatus(location.getLocation(), request.geocodeStatus());
            location.setGeocodeStatus(request.geocodeStatus());
        }
        if (request.headquarters() != null) {
            if (request.headquarters()) {
                clearHeadquarters(companyId, locationId);
            }
            location.setHeadquarters(request.headquarters());
        }

        return CompanyLocationResponse.from(locationRepository.save(location));
    }

    private AdminCompanyResponse changeStatus(UUID id, RecordStatus status) {
        Company company = findCompany(id);
        requireNotMerged(company);
        company.setRecordStatus(status);
        return toResponse(companyRepository.save(company));
    }

    private Company findCompany(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private String resolveSlug(String requestedSlug, String name, UUID excludeId) {
        if (requestedSlug != null) {
            String slug = TextNormalizer.slugify(requestedSlug);
            if (slug == null) {
                throw InvalidParameterException.of("slug", "must contain at least one letter or number");
            }
            ensureSlugAvailable(slug, excludeId);
            return slug;
        }

        String baseSlug = TextNormalizer.slugify(name);
        if (baseSlug == null) {
            throw InvalidParameterException.of("name", "cannot be converted to a URL slug; provide slug explicitly");
        }
        String slug = baseSlug;
        int suffix = 2;
        while (slugInUse(slug, excludeId)) {
            slug = baseSlug + "-" + suffix++;
        }
        return slug;
    }

    private boolean slugInUse(String slug, UUID excludeId) {
        return companyRepository.findBySlug(slug)
                .filter(company -> !company.getId().equals(excludeId))
                .isPresent();
    }

    private void ensureSlugAvailable(String slug, UUID excludeId) {
        if (slugInUse(slug, excludeId)) {
            throw new ConflictException("A company with slug '%s' already exists".formatted(slug));
        }
    }

    private void ensureNameAvailable(String normalizedName, UUID excludeId) {
        var existing = excludeId == null
                ? companyRepository.findFirstByNormalizedName(normalizedName)
                : companyRepository.findFirstByNormalizedNameAndIdNot(normalizedName, excludeId);
        existing.ifPresent(company -> {
            throw new ConflictException("A company with the same normalized name already exists: %s"
                    .formatted(company.getId()));
        });
    }

    private void ensureDomainAvailable(String websiteDomain, UUID excludeId) {
        if (websiteDomain == null) {
            return;
        }
        var existing = excludeId == null
                ? companyRepository.findFirstByWebsiteDomainIgnoreCase(websiteDomain)
                : companyRepository.findFirstByWebsiteDomainIgnoreCaseAndIdNot(websiteDomain, excludeId);
        existing.ifPresent(company -> {
            throw new ConflictException("A company with the same website domain already exists: %s"
                    .formatted(company.getId()));
        });
    }

    private Industry resolveIndustry(String industrySlug) {
        String slug = trimToNull(industrySlug);
        if (slug == null) {
            return null;
        }
        return industryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Industry", slug));
    }

    private void validateFoundedYear(Short foundedYear) {
        if (foundedYear != null && foundedYear > Year.now().getValue()) {
            throw InvalidParameterException.of("foundedYear", "must not be in the future");
        }
    }

    private void validateEmployeeRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw InvalidParameterException.of("employeeCountMin", "must be less than or equal to employeeCountMax");
        }
    }

    private void clearHeadquarters(UUID companyId, UUID exceptLocationId) {
        for (CompanyLocation existing : locationRepository.findByCompanyIdOrderByHeadquartersDesc(companyId)) {
            if (existing.isHeadquarters() && !existing.getId().equals(exceptLocationId)) {
                existing.setHeadquarters(false);
            }
        }
    }

    private void applyLocationFields(CompanyLocation entity, String locationName, String addressLine, String area,
                                     String city, String state, String countryCode, String postalCode,
                                     LocationType locationType) {
        entity.setLocationName(trimToNull(locationName));
        entity.setAddressLine(trimToNull(addressLine));
        entity.setArea(trimToNull(area));
        entity.setCity(requireText(city, "city"));
        entity.setState(requireText(state, "state"));
        entity.setCountryCode(countryCode.trim().toUpperCase(Locale.ROOT));
        entity.setPostalCode(trimToNull(postalCode));
        entity.setLocationType(locationType == null ? LocationType.OFFICE : locationType);
    }

    private Point buildPoint(Double latitude, Double longitude, GeocodeStatus geocodeStatus) {
        if ((latitude == null) != (longitude == null)) {
            throw InvalidParameterException.of("latitude", "latitude and longitude must be provided together");
        }
        validateGeocodeStatus(latitude == null ? null : GEOMETRY_FACTORY.createPoint(
                new Coordinate(longitude, latitude)), geocodeStatus);
        if (latitude == null) {
            return null;
        }
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private void validateGeocodeStatus(Point point, GeocodeStatus geocodeStatus) {
        if ((geocodeStatus == GeocodeStatus.VERIFIED || geocodeStatus == GeocodeStatus.APPROXIMATE)
                && point == null) {
            throw InvalidParameterException.of("geocodeStatus",
                    geocodeStatus + " locations require latitude and longitude");
        }
    }

    private AdminCompanySummaryResponse toSummary(Company company) {
        return new AdminCompanySummaryResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getWebsiteUrl(),
                company.getWebsiteDomain(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getCompanyType(),
                company.getVerificationStatus(),
                company.getRecordStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }

    private AdminCompanyResponse toResponse(Company company) {
        List<CompanyLocationResponse> locations =
                locationRepository.findByCompanyIdOrderByHeadquartersDesc(company.getId()).stream()
                        .map(CompanyLocationResponse::from)
                        .toList();
        List<AdminCompanySourceResponse> sources =
                sourceRepository.findByCompanyIdOrderByCreatedAtAsc(company.getId()).stream()
                        .map(AdminCompanySourceResponse::from)
                        .toList();
        return new AdminCompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getWebsiteUrl(),
                company.getWebsiteDomain(),
                company.getLogoUrl(),
                company.getDescription(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getIndustry() == null ? null : company.getIndustry().getSlug(),
                company.getFoundedYear(),
                company.getEmployeeCountMin(),
                company.getEmployeeCountMax(),
                company.getFundingStage(),
                company.getCompanyType(),
                company.getVerificationStatus(),
                company.getRecordStatus(),
                locations,
                sources,
                company.getCreatedAt(),
                company.getUpdatedAt());
    }

    private Specification<Company> toSpecification(String keyword, RecordStatus recordStatus,
                                                   VerificationStatus verificationStatus, String industry) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (recordStatus != null) {
                predicates.add(cb.equal(root.get("recordStatus"), recordStatus));
            }
            if (verificationStatus != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), verificationStatus));
            }
            String normalizedKeyword = TextNormalizer.normalizeName(keyword);
            if (normalizedKeyword != null && !normalizedKeyword.isEmpty()) {
                predicates.add(cb.like(root.get("normalizedName"), TextNormalizer.likePattern(normalizedKeyword), '\\'));
            }
            String normalizedIndustry = TextNormalizer.normalizeName(industry);
            if (normalizedIndustry != null && !normalizedIndustry.isEmpty()) {
                Join<Company, Industry> industryJoin = root.join("industry", JoinType.INNER);
                predicates.add(cb.or(
                        cb.equal(cb.lower(industryJoin.get("slug")), normalizedIndustry),
                        cb.equal(cb.lower(industryJoin.get("name")), normalizedIndustry)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort sort(String sort) {
        return switch (sort == null ? "name" : sort) {
            case "name" -> Sort.by(Sort.Order.asc("normalizedName"));
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "updated" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> throw InvalidParameterException.of(
                    "sort", "allowed values are 'name', 'newest' and 'updated'");
        };
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

    private String websiteDomain(String websiteUrl) {
        if (websiteUrl == null) {
            return null;
        }
        try {
            String host = URI.create(websiteUrl).getHost();
            if (host == null) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            while (host.startsWith("www.")) {
                host = host.substring(4);
            }
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            return host.isEmpty() ? null : host;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void requireNotMerged(Company company) {
        if (company.getRecordStatus() == RecordStatus.MERGED) {
            throw InvalidParameterException.of("recordStatus", "merged companies cannot be changed");
        }
    }
}
