package com.punestartup.company.controller;

import com.punestartup.common.web.PageResponse;
import com.punestartup.company.dto.AdminCompanyResponse;
import com.punestartup.company.dto.AdminCompanySummaryResponse;
import com.punestartup.company.dto.CompanyLocationResponse;
import com.punestartup.company.dto.CreateCompanyLocationRequest;
import com.punestartup.company.dto.CreateCompanyRequest;
import com.punestartup.company.dto.UpdateCompanyLocationRequest;
import com.punestartup.company.dto.UpdateCompanyRequest;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import com.punestartup.company.service.AdminCompanyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/companies")
public class AdminCompanyController {

    private final AdminCompanyService companyService;

    public AdminCompanyController(AdminCompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public PageResponse<AdminCompanySummaryResponse> listCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RecordStatus recordStatus,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return PageResponse.from(companyService.listCompanies(keyword, recordStatus, verificationStatus, industry,
                sort, page, size));
    }

    @GetMapping("/duplicates")
    public List<AdminCompanySummaryResponse> findDuplicates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String websiteUrl,
            @RequestParam(required = false) UUID excludeId) {
        return companyService.findDuplicates(name, websiteUrl, excludeId);
    }

    @GetMapping("/{id}")
    public AdminCompanyResponse getCompany(@PathVariable UUID id) {
        return companyService.getCompany(id);
    }

    @PostMapping
    public ResponseEntity<AdminCompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        AdminCompanyResponse created = companyService.createCompany(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/companies/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public AdminCompanyResponse updateCompany(@PathVariable UUID id,
                                              @Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.updateCompany(id, request);
    }

    @PostMapping("/{id}/publish")
    public AdminCompanyResponse publishCompany(@PathVariable UUID id) {
        return companyService.publishCompany(id);
    }

    @PostMapping("/{id}/unpublish")
    public AdminCompanyResponse unpublishCompany(@PathVariable UUID id) {
        return companyService.unpublishCompany(id);
    }

    @PostMapping("/{id}/archive")
    public AdminCompanyResponse archiveCompany(@PathVariable UUID id) {
        return companyService.archiveCompany(id);
    }

    @PostMapping("/{id}/locations")
    public ResponseEntity<CompanyLocationResponse> addLocation(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCompanyLocationRequest request) {
        CompanyLocationResponse created = companyService.addLocation(id, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/companies/" + id + "/locations/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{id}/locations/{locationId}")
    public CompanyLocationResponse updateLocation(
            @PathVariable UUID id,
            @PathVariable UUID locationId,
            @Valid @RequestBody UpdateCompanyLocationRequest request) {
        return companyService.updateLocation(id, locationId, request);
    }
}
