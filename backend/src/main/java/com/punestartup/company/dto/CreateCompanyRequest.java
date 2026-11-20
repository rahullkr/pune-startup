package com.punestartup.company.dto;

import com.punestartup.company.entity.CompanyType;
import com.punestartup.company.entity.VerificationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 220)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must contain lowercase letters, numbers and hyphens only")
        String slug,
        @Size(max = 2048)
        @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL")
        String websiteUrl,
        @Size(max = 2048)
        @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL")
        String logoUrl,
        @Size(max = 20000) String description,
        @Size(max = 120) String industrySlug,
        @Min(1800) @Max(2100) Short foundedYear,
        @Min(0) Integer employeeCountMin,
        @Min(0) Integer employeeCountMax,
        @Size(max = 50) String fundingStage,
        CompanyType companyType,
        VerificationStatus verificationStatus) {
}
