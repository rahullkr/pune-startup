package com.punestartup.company.dto;

import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminCompanyResponse(
        UUID id,
        String name,
        String slug,
        String websiteUrl,
        String websiteDomain,
        String logoUrl,
        String description,
        String industryName,
        String industrySlug,
        Short foundedYear,
        Integer employeeCountMin,
        Integer employeeCountMax,
        String fundingStage,
        CompanyType companyType,
        VerificationStatus verificationStatus,
        RecordStatus recordStatus,
        List<CompanyLocationResponse> locations,
        List<AdminCompanySourceResponse> sources,
        Instant createdAt,
        Instant updatedAt) {
}
