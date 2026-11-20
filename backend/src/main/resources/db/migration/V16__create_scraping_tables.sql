CREATE TABLE scraping_sources (
    id                         UUID PRIMARY KEY,
    source_name                VARCHAR(150) NOT NULL,
    source_type                VARCHAR(40) NOT NULL,
    base_url                   TEXT NOT NULL,
    company_id                 UUID REFERENCES companies (id),
    permission_review_status   VARCHAR(30) NOT NULL,
    crawl_interval_minutes     INTEGER NOT NULL,
    rate_limit_per_minute      INTEGER,
    is_enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    last_success_at            TIMESTAMPTZ,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_scraping_sources_permission_status CHECK (
        permission_review_status IN ('PENDING', 'APPROVED', 'REJECTED')
    ),
    CONSTRAINT chk_scraping_sources_interval CHECK (crawl_interval_minutes > 0),
    CONSTRAINT chk_scraping_sources_rate_limit CHECK (rate_limit_per_minute IS NULL OR rate_limit_per_minute > 0)
);

CREATE INDEX idx_scraping_sources_company ON scraping_sources (company_id);

CREATE TABLE scraping_runs (
    id                   UUID PRIMARY KEY,
    scraping_source_id   UUID NOT NULL REFERENCES scraping_sources (id),
    status               VARCHAR(30) NOT NULL,
    started_at           TIMESTAMPTZ NOT NULL,
    finished_at          TIMESTAMPTZ,
    records_discovered   INTEGER NOT NULL DEFAULT 0,
    records_validated    INTEGER NOT NULL DEFAULT 0,
    records_rejected     INTEGER NOT NULL DEFAULT 0,
    error_summary        TEXT,
    CONSTRAINT chk_scraping_runs_status CHECK (
        status IN ('RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')
    ),
    CONSTRAINT chk_scraping_runs_counts CHECK (
        records_discovered >= 0 AND records_validated >= 0 AND records_rejected >= 0
    )
);

CREATE INDEX idx_scraping_runs_source ON scraping_runs (scraping_source_id, started_at DESC);

CREATE TABLE scraped_records (
    id                  UUID PRIMARY KEY,
    scraping_run_id     UUID NOT NULL REFERENCES scraping_runs (id),
    record_type         VARCHAR(30) NOT NULL,
    external_id         VARCHAR(255),
    source_url          TEXT,
    payload             JSONB NOT NULL,
    processing_status   VARCHAR(30) NOT NULL,
    processed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_scraped_records_type CHECK (
        record_type IN ('COMPANY', 'JOB')
    ),
    CONSTRAINT chk_scraped_records_status CHECK (
        processing_status IN ('PENDING', 'PROCESSED', 'FAILED', 'SKIPPED')
    )
);

CREATE INDEX idx_scraped_records_run ON scraped_records (scraping_run_id);
