CREATE TABLE saved_jobs (
    candidate_profile_id   UUID NOT NULL REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    job_id                 UUID NOT NULL REFERENCES jobs (id),
    saved_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_saved_jobs PRIMARY KEY (candidate_profile_id, job_id)
);

CREATE INDEX idx_saved_jobs_job ON saved_jobs (job_id);

CREATE TABLE followed_companies (
    candidate_profile_id   UUID NOT NULL REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    company_id             UUID NOT NULL REFERENCES companies (id),
    followed_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_followed_companies PRIMARY KEY (candidate_profile_id, company_id)
);

CREATE INDEX idx_followed_companies_company ON followed_companies (company_id);
