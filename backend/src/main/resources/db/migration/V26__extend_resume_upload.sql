ALTER TABLE resumes
    ADD COLUMN consent_given_at TIMESTAMPTZ,
    ADD COLUMN consent_version VARCHAR(50),
    ADD COLUMN retention_expires_at TIMESTAMPTZ,
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN deleted_reason VARCHAR(40),
    ADD COLUMN parse_error_code VARCHAR(50),
    ADD COLUMN parsed_skill_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE resumes
SET consent_given_at = uploaded_at,
    consent_version = 'legacy',
    retention_expires_at = uploaded_at + INTERVAL '30 days'
WHERE consent_given_at IS NULL;

ALTER TABLE resumes
    ALTER COLUMN consent_given_at SET NOT NULL,
    ALTER COLUMN consent_version SET NOT NULL,
    ALTER COLUMN retention_expires_at SET NOT NULL,
    ADD CONSTRAINT chk_resumes_deleted_reason CHECK (
        deleted_reason IS NULL OR deleted_reason IN (
            'CANDIDATE_REQUEST',
            'ACCOUNT_DEACTIVATED',
            'RETENTION_EXPIRED',
            'REPLACED'
        )
    ),
    ADD CONSTRAINT chk_resumes_parsed_skill_count CHECK (parsed_skill_count >= 0);

CREATE INDEX idx_resumes_active_candidate ON resumes (candidate_profile_id, uploaded_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_resumes_retention ON resumes (retention_expires_at)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_resumes_primary_candidate ON resumes (candidate_profile_id)
    WHERE is_primary AND deleted_at IS NULL;
