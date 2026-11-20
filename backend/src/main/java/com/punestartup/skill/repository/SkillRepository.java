package com.punestartup.skill.repository;

import com.punestartup.skill.entity.Skill;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findByNormalizedName(String normalizedName);

    @Query(value = """
            SELECT DISTINCT s.*
            FROM skills s
            JOIN job_skills js ON js.skill_id = s.id
            JOIN jobs j ON j.id = js.job_id
            JOIN companies c ON c.id = j.company_id
            WHERE j.status = 'ACTIVE'
              AND c.record_status = 'ACTIVE'
            """, nativeQuery = true)
    List<Skill> findSkillsUsedByActiveJobs();
}
