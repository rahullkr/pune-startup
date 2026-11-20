CREATE TABLE applications (
    id                        UUID PRIMARY KEY,
    candidate_profile_id      UUID NOT NULL REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    job_id                    UUID NOT NULL REFERENCES jobs (id),
    status                    VARCHAR(30) NOT NULL,
    applied_at                TIMESTAMPTZ,
    external_application_url   TEXT,
    notes                     TEXT,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_applications_candidate_job UNIQUE (candidate_profile_id, job_id),
    CONSTRAINT chk_applications_status CHECK (
        status IN ('SAVED', 'APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED', 'ACCEPTED', 'DECLINED', 'WITHDRAWN')
    )
);

CREATE INDEX idx_applications_job ON applications (job_id);

CREATE TABLE application_status_history (
    id                UUID PRIMARY KEY,
    application_id    UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    old_status        VARCHAR(30),
    new_status        VARCHAR(30) NOT NULL,
    changed_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    note              TEXT
);

CREATE INDEX idx_application_status_history_application ON application_status_history (application_id, changed_at DESC);
