CREATE INDEX idx_jobs_company_normalized_title
    ON jobs (company_id, normalized_title);

CREATE INDEX idx_jobs_status_updated
    ON jobs (status, updated_at DESC);

CREATE INDEX idx_job_sources_application_url
    ON job_sources (application_url);

CREATE INDEX idx_job_sources_source_url
    ON job_sources (source_url);
