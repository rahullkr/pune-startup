ALTER TABLE scraping_runs
    ADD COLUMN retried_from_run_id UUID REFERENCES scraping_runs (id) ON DELETE SET NULL;

CREATE INDEX idx_scraping_runs_retried_from ON scraping_runs (retried_from_run_id);

ALTER TABLE scraping_sources
    ADD COLUMN consecutive_failed_runs INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_failure_at TIMESTAMPTZ;

ALTER TABLE scraping_sources
    ADD CONSTRAINT chk_scraping_sources_failed_runs CHECK (consecutive_failed_runs >= 0);
