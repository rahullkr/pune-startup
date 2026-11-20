package com.punestartup.ingestion.service;

import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.ResourceNotFoundException;
import com.punestartup.common.util.TextNormalizer;
import com.punestartup.common.web.PageRequests;
import com.punestartup.company.entity.Company;
import com.punestartup.company.repository.CompanyRepository;
import com.punestartup.ingestion.dto.CreateScrapingSourceRequest;
import com.punestartup.ingestion.dto.ScrapingSourceResponse;
import com.punestartup.ingestion.dto.UpdateScrapingSourceRequest;
import com.punestartup.ingestion.entity.PermissionReviewStatus;
import com.punestartup.ingestion.entity.ScrapingSource;
import com.punestartup.ingestion.entity.ScrapingSourceType;
import com.punestartup.ingestion.repository.ScrapingSourceRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScrapingSourceService {

    private final ScrapingSourceRepository sourceRepository;
    private final CompanyRepository companyRepository;
    private final int maxPageSize;

    public ScrapingSourceService(ScrapingSourceRepository sourceRepository,
                                 CompanyRepository companyRepository,
                                 @Value("${app.search.max-page-size}") int maxPageSize) {
        this.sourceRepository = sourceRepository;
        this.companyRepository = companyRepository;
        this.maxPageSize = maxPageSize;
    }

    @Transactional(readOnly = true)
    public Page<ScrapingSourceResponse> listSources(Boolean enabled, PermissionReviewStatus permissionStatus,
                                                    ScrapingSourceType sourceType, String keyword,
                                                    int page, int size) {
        PageRequest pageRequest = PageRequests.of(page, size,
                Sort.by(Sort.Order.asc("sourceName").ignoreCase()), maxPageSize);
        return sourceRepository.findAll(toSpecification(enabled, permissionStatus, sourceType, keyword), pageRequest)
                .map(ScrapingSourceResponse::from);
    }

    @Transactional(readOnly = true)
    public ScrapingSourceResponse getSource(UUID id) {
        return ScrapingSourceResponse.from(findSource(id));
    }

    @Transactional
    public ScrapingSourceResponse createSource(CreateScrapingSourceRequest request) {
        PermissionReviewStatus status = request.permissionReviewStatus() == null
                ? PermissionReviewStatus.PENDING
                : request.permissionReviewStatus();
        boolean enabled = Boolean.TRUE.equals(request.enabled());
        validateEnabled(enabled, status);

        ScrapingSource source = new ScrapingSource();
        source.setId(UUID.randomUUID());
        source.setSourceName(request.sourceName().trim());
        source.setSourceType(request.sourceType());
        source.setBaseUrl(request.baseUrl().trim());
        source.setCompany(resolveCompany(request.companyId()));
        source.setPermissionReviewStatus(status);
        source.setPermissionNotes(trimToNull(request.permissionNotes()));
        source.setCrawlIntervalMinutes(request.crawlIntervalMinutes());
        source.setRateLimitPerMinute(request.rateLimitPerMinute());
        source.setEnabled(enabled);
        return ScrapingSourceResponse.from(sourceRepository.save(source));
    }

    @Transactional
    public ScrapingSourceResponse updateSource(UUID id, UpdateScrapingSourceRequest request) {
        ScrapingSource source = findSource(id);

        if (request.sourceName() != null) {
            String sourceName = request.sourceName().trim();
            if (sourceName.isEmpty()) {
                throw InvalidParameterException.of("sourceName", "must not be blank");
            }
            source.setSourceName(sourceName);
        }
        if (request.sourceType() != null) {
            source.setSourceType(request.sourceType());
        }
        if (request.baseUrl() != null) {
            source.setBaseUrl(request.baseUrl().trim());
        }
        if (request.companyId() != null) {
            source.setCompany(resolveCompany(request.companyId()));
        }
        if (request.permissionNotes() != null) {
            source.setPermissionNotes(trimToNull(request.permissionNotes()));
        }
        if (request.crawlIntervalMinutes() != null) {
            source.setCrawlIntervalMinutes(request.crawlIntervalMinutes());
        }
        if (request.rateLimitPerMinute() != null) {
            source.setRateLimitPerMinute(request.rateLimitPerMinute());
        }

        PermissionReviewStatus status = request.permissionReviewStatus() == null
                ? source.getPermissionReviewStatus()
                : request.permissionReviewStatus();
        if (status != PermissionReviewStatus.APPROVED) {
            if (Boolean.TRUE.equals(request.enabled())) {
                throw InvalidParameterException.of("enabled", "only APPROVED sources can be enabled");
            }
            source.setEnabled(false);
        } else if (request.enabled() != null) {
            source.setEnabled(request.enabled());
        }
        source.setPermissionReviewStatus(status);

        return ScrapingSourceResponse.from(sourceRepository.save(source));
    }

    private ScrapingSource findSource(UUID id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping source", id));
    }

    private Company resolveCompany(UUID companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));
    }

    private void validateEnabled(boolean enabled, PermissionReviewStatus status) {
        if (enabled && status != PermissionReviewStatus.APPROVED) {
            throw InvalidParameterException.of("enabled", "only APPROVED sources can be enabled");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Specification<ScrapingSource> toSpecification(Boolean enabled, PermissionReviewStatus permissionStatus,
                                                          ScrapingSourceType sourceType, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            if (permissionStatus != null) {
                predicates.add(cb.equal(root.get("permissionReviewStatus"), permissionStatus));
            }
            if (sourceType != null) {
                predicates.add(cb.equal(root.get("sourceType"), sourceType));
            }
            String normalizedKeyword = TextNormalizer.normalizeName(keyword);
            if (normalizedKeyword != null && !normalizedKeyword.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("sourceName")),
                        TextNormalizer.likePattern(normalizedKeyword), '\\'));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
