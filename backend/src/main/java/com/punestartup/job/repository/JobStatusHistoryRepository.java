package com.punestartup.job.repository;

import com.punestartup.job.entity.JobStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobStatusHistoryRepository extends JpaRepository<JobStatusHistory, UUID> {

    List<JobStatusHistory> findByJob_IdOrderByChangedAtAsc(UUID jobId);
}
