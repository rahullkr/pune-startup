package com.punestartup.company.repository;

import com.punestartup.company.entity.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

    Optional<Company> findBySlug(String slug);

    Optional<Company> findFirstByNormalizedName(String normalizedName);

    Optional<Company> findFirstByWebsiteUrlIgnoreCase(String websiteUrl);

    Optional<Company> findFirstByNormalizedNameAndIdNot(String normalizedName, UUID id);

    Optional<Company> findFirstByWebsiteDomainIgnoreCase(String websiteDomain);

    Optional<Company> findFirstByWebsiteDomainIgnoreCaseAndIdNot(String websiteDomain, UUID id);
}
