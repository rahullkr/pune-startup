package com.punestartup.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class JobSkillId implements Serializable {

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;
}
