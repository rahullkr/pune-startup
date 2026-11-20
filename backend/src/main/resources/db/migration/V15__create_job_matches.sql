CREATE TABLE job_matches (
    id                     UUID PRIMARY KEY,
    candidate_profile_id   UUID NOT NULL REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    job_id                 UUID NOT NULL REFERENCES jobs (id),
    match_score            NUMERIC(5,2) NOT NULL,
    skill_score            NUMERIC(5,2),
    experience_score       NUMERIC(5,2),
    location_score         NUMERIC(5,2),
    work_mode_score        NUMERIC(5,2),
    explanation            JSONB,
    algorithm_version      VARCHAR(40) NOT NULL,
    calculated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_job_matches_candidate_job_version UNIQUE (candidate_profile_id, job_id, algorithm_version),
    CONSTRAINT chk_job_matches_score CHECK (match_score >= 0 AND match_score <= 100),
    CONSTRAINT chk_job_matches_skill_score CHECK (skill_score IS NULL OR (skill_score >= 0 AND skill_score <= 100)),
    CONSTRAINT chk_job_matches_experience_score CHECK (experience_score IS NULL OR (experience_score >= 0 AND experience_score <= 100)),
    CONSTRAINT chk_job_matches_location_score CHECK (location_score IS NULL OR (location_score >= 0 AND location_score <= 100)),
    CONSTRAINT chk_job_matches_work_mode_score CHECK (work_mode_score IS NULL OR (work_mode_score >= 0 AND work_mode_score <= 100))
);

CREATE INDEX idx_job_matches_candidate ON job_matches (candidate_profile_id, match_score DESC);
