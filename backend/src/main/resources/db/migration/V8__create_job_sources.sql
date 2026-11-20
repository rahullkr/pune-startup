CREATE TABLE job_sources (
    id                            UUID PRIMARY KEY,
    job_id                        UUID NOT NULL REFERENCES jobs (id),
    source_name                   VARCHAR(100) NOT NULL,
    source_job_id                 VARCHAR(255),
    source_url                    TEXT NOT NULL,
    application_url               TEXT,
    source_status                 VARCHAR(30) NOT NULL,
    first_seen_at                 TIMESTAMPTZ NOT NULL,
    last_seen_at                  TIMESTAMPTZ NOT NULL,
    last_checked_at               TIMESTAMPTZ,
    consecutive_missing_checks    INTEGER NOT NULL DEFAULT 0,
    raw_metadata                  JSONB,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_job_sources_source_status CHECK (
        source_status IN ('DISCOVERED', 'ACTIVE', 'MISSING', 'REMOVED')
    ),
    CONSTRAINT chk_job_sources_missing_checks CHECK (consecutive_missing_checks >= 0)
);

CREATE UNIQUE INDEX uq_job_sources_source_identifier
    ON job_sources (source_name, source_job_id)
    WHERE source_job_id IS NOT NULL;

CREATE INDEX idx_job_sources_job ON job_sources (job_id);
