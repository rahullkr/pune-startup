package com.punestartup.job.dto;

import com.punestartup.company.entity.Company;
import java.util.UUID;

public record JobCompanyResponse(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String websiteUrl,
        String industryName,
        String verificationStatus) {

    public static JobCompanyResponse from(Company company) {
        return new JobCompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getLogoUrl(),
                company.getWebsiteUrl(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getVerificationStatus() == null ? null : company.getVerificationStatus().name());
    }
}
