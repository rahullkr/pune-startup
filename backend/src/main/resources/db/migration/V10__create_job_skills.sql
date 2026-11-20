CREATE TABLE job_skills (
    job_id             UUID NOT NULL REFERENCES jobs (id),
    skill_id           UUID NOT NULL REFERENCES skills (id),
    requirement_type   VARCHAR(20) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_job_skills PRIMARY KEY (job_id, skill_id),
    CONSTRAINT chk_job_skills_requirement_type CHECK (
        requirement_type IN ('REQUIRED', 'PREFERRED')
    )
);

CREATE INDEX idx_job_skills_skill ON job_skills (skill_id, job_id);
