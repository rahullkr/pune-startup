ALTER TABLE jobs
    ADD COLUMN first_seen_at TIMESTAMPTZ,
    ADD COLUMN last_seen_at TIMESTAMPTZ,
    ADD COLUMN last_checked_at TIMESTAMPTZ,
    ADD COLUMN stale_since TIMESTAMPTZ,
    ADD COLUMN expired_at TIMESTAMPTZ;

UPDATE jobs
SET first_seen_at = created_at,
    last_seen_at = COALESCE(last_verified_at, first_published_at, created_at),
    last_checked_at = last_verified_at;

CREATE INDEX idx_jobs_status_last_checked
    ON jobs (status, last_checked_at);

ALTER TABLE job_sources
    ADD COLUMN scraping_source_id UUID REFERENCES scraping_sources (id),
    ADD COLUMN last_missing_at TIMESTAMPTZ,
    ADD COLUMN last_missing_run_id UUID REFERENCES scraping_runs (id),
    ADD COLUMN removed_at TIMESTAMPTZ;

UPDATE job_sources js
SET scraping_source_id = matched.id
FROM (
    SELECT DISTINCT ON (source_name) source_name, id
    FROM scraping_sources
    ORDER BY source_name, created_at, id
) AS matched
WHERE js.scraping_source_id IS NULL
  AND js.source_name = matched.source_name;

CREATE INDEX idx_job_sources_scraping_source
    ON job_sources (scraping_source_id);

CREATE INDEX idx_job_sources_job_status
    ON job_sources (job_id, source_status);

ALTER TABLE job_status_history
    ADD COLUMN scraping_run_id UUID REFERENCES scraping_runs (id);

ALTER TABLE scraping_runs
    ADD COLUMN records_emitted INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN records_processed INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN freshness_reconciled_at TIMESTAMPTZ;

ALTER TABLE scraping_runs
    ADD CONSTRAINT chk_scraping_runs_records_emitted CHECK (records_emitted >= 0),
    ADD CONSTRAINT chk_scraping_runs_records_processed CHECK (records_processed >= 0);

UPDATE scraping_runs
SET records_emitted = records_discovered,
    records_processed = records_discovered;
