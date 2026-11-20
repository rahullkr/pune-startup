package com.punestartup.company.dto;

import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.RecordStatus;
import com.punestartup.company.entity.VerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminCompanySummaryResponse(
        UUID id,
        String name,
        String slug,
        String websiteUrl,
        String websiteDomain,
        String industryName,
        CompanyType companyType,
        VerificationStatus verificationStatus,
        RecordStatus recordStatus,
        Instant createdAt,
        Instant updatedAt) {
}
