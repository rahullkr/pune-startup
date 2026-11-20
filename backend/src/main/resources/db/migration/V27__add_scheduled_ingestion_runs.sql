ALTER TABLE scraping_runs
    ADD COLUMN trigger_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE scraping_runs
    DROP CONSTRAINT chk_scraping_runs_status;

ALTER TABLE scraping_runs
    ADD CONSTRAINT chk_scraping_runs_status CHECK (
        status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED', 'SKIPPED', 'MISSED')
    );

ALTER TABLE scraping_runs
    ADD CONSTRAINT chk_scraping_runs_trigger_type CHECK (
        trigger_type IN ('MANUAL', 'SCHEDULED', 'RETRY')
    );

CREATE INDEX idx_scraping_runs_source_status_started
    ON scraping_runs (scraping_source_id, status, started_at DESC);
