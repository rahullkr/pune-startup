CREATE TABLE jobs (
    id                     UUID PRIMARY KEY,
    company_id             UUID NOT NULL REFERENCES companies (id),
    company_location_id    UUID REFERENCES company_locations (id),
    title                  VARCHAR(250) NOT NULL,
    normalized_title       VARCHAR(250) NOT NULL,
    description            TEXT,
    employment_type        VARCHAR(30) NOT NULL,
    work_mode              VARCHAR(20) NOT NULL,
    experience_min_years   NUMERIC(4,1),
    experience_max_years   NUMERIC(4,1),
    salary_min             NUMERIC(12,2),
    salary_max             NUMERIC(12,2),
    salary_currency        CHAR(3),
    salary_period          VARCHAR(20),
    status                 VARCHAR(20) NOT NULL,
    first_published_at     TIMESTAMPTZ,
    last_verified_at       TIMESTAMPTZ,
    expires_at             TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_jobs_employment_type CHECK (
        employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'TEMPORARY')
    ),
    CONSTRAINT chk_jobs_work_mode CHECK (
        work_mode IN ('ONSITE', 'HYBRID', 'REMOTE', 'UNKNOWN')
    ),
    CONSTRAINT chk_jobs_status CHECK (
        status IN ('DISCOVERED', 'NEEDS_REVIEW', 'ACTIVE', 'STALE', 'EXPIRED', 'REJECTED')
    ),
    CONSTRAINT chk_jobs_experience_min CHECK (experience_min_years IS NULL OR experience_min_years >= 0),
    CONSTRAINT chk_jobs_experience_max CHECK (experience_max_years IS NULL OR experience_max_years >= 0),
    CONSTRAINT chk_jobs_experience_range CHECK (
        experience_min_years IS NULL OR experience_max_years IS NULL OR experience_min_years <= experience_max_years
    ),
    CONSTRAINT chk_jobs_salary_min CHECK (salary_min IS NULL OR salary_min >= 0),
    CONSTRAINT chk_jobs_salary_max CHECK (salary_max IS NULL OR salary_max >= 0),
    CONSTRAINT chk_jobs_salary_range CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max
    ),
    CONSTRAINT chk_jobs_salary_currency CHECK (
        (salary_min IS NULL AND salary_max IS NULL)
        OR (salary_currency IS NOT NULL AND salary_period IS NOT NULL)
    ),
    CONSTRAINT chk_jobs_salary_currency_format CHECK (salary_currency IS NULL OR salary_currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_jobs_company ON jobs (company_id);
CREATE INDEX idx_jobs_status_created ON jobs (status, created_at DESC);
CREATE INDEX idx_jobs_normalized_title ON jobs (normalized_title);
