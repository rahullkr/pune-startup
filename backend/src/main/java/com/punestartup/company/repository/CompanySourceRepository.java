package com.punestartup.company.repository;

import com.punestartup.company.entity.CompanySource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySourceRepository extends JpaRepository<CompanySource, UUID> {

    List<CompanySource> findByCompanyIdOrderByCreatedAtAsc(UUID companyId);
}
