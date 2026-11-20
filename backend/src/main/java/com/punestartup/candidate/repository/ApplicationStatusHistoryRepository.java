package com.punestartup.candidate.repository;

import com.punestartup.candidate.entity.ApplicationStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository
        extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findByApplication_IdOrderByChangedAtAsc(UUID applicationId);

    void deleteByApplication_Id(UUID applicationId);
}
