package com.punestartup.job.repository;

import com.punestartup.job.entity.JobSkill;
import com.punestartup.job.entity.JobSkillId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {

    List<JobSkill> findByJob_Id(UUID jobId);
}
