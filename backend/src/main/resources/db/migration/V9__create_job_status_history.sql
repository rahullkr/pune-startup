CREATE TABLE job_status_history (
    id                   UUID PRIMARY KEY,
    job_id               UUID NOT NULL REFERENCES jobs (id),
    old_status           VARCHAR(20),
    new_status           VARCHAR(20) NOT NULL,
    changed_by_user_id   UUID,
    reason               TEXT,
    changed_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_status_history_job ON job_status_history (job_id, changed_at DESC);
