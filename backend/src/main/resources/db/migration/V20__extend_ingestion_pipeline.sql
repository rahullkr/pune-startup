ALTER TABLE job_sources
    ALTER COLUMN source_name TYPE VARCHAR(150);

ALTER TABLE scraping_runs
    ADD COLUMN records_duplicated INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN records_failed INTEGER NOT NULL DEFAULT 0;

ALTER TABLE scraping_runs
    ADD CONSTRAINT chk_scraping_runs_extended_counts CHECK (
        records_duplicated >= 0 AND records_failed >= 0
    );

CREATE UNIQUE INDEX uq_scraping_runs_one_running
    ON scraping_runs (scraping_source_id)
    WHERE status = 'RUNNING';

ALTER TABLE scraped_records
    ADD COLUMN event_id UUID,
    ADD COLUMN job_id UUID REFERENCES jobs (id),
    ADD COLUMN error_message TEXT,
    ADD COLUMN normalized_payload JSONB;

CREATE UNIQUE INDEX uq_scraped_records_event
    ON scraped_records (event_id)
    WHERE event_id IS NOT NULL;

CREATE INDEX idx_scraped_records_status
    ON scraped_records (processing_status, created_at DESC);

CREATE INDEX idx_scraped_records_job
    ON scraped_records (job_id);

CREATE TABLE processed_ingestion_events (
    event_id      UUID PRIMARY KEY,
    event_type    VARCHAR(60) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_ingestion_events_processed
    ON processed_ingestion_events (processed_at DESC);
