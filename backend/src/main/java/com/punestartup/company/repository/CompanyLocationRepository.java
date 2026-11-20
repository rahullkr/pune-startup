package com.punestartup.company.repository;

import com.punestartup.company.entity.CompanyLocation;
import com.punestartup.company.entity.RecordStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyLocationRepository extends JpaRepository<CompanyLocation, UUID> {

    List<CompanyLocation> findByCompanyIdOrderByHeadquartersDesc(UUID companyId);

    List<CompanyLocation> findByCompanyIdIn(Collection<UUID> companyIds);

    @Query("""
            select distinct l.area
            from CompanyLocation l
            where l.company.recordStatus = :recordStatus
              and l.area is not null
              and l.area <> ''
            order by l.area
            """)
    List<String> findDistinctAreasByCompanyRecordStatus(@Param("recordStatus") RecordStatus recordStatus);
}
