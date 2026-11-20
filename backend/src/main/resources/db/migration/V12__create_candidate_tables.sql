CREATE TABLE candidate_profiles (
    id                        UUID PRIMARY KEY,
    user_id                   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    full_name                 VARCHAR(200),
    headline                  VARCHAR(250),
    summary                   TEXT,
    total_experience_months   INTEGER,
    preferred_city            VARCHAR(100),
    preferred_work_mode       VARCHAR(20),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_candidate_profiles_user UNIQUE (user_id),
    CONSTRAINT chk_candidate_profiles_experience CHECK (
        total_experience_months IS NULL OR total_experience_months >= 0
    ),
    CONSTRAINT chk_candidate_profiles_work_mode CHECK (
        preferred_work_mode IS NULL OR preferred_work_mode IN ('ONSITE', 'HYBRID', 'REMOTE')
    )
);

CREATE TABLE candidate_skills (
    candidate_profile_id   UUID NOT NULL REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    skill_id               UUID NOT NULL REFERENCES skills (id),
    proficiency            VARCHAR(30),
    years_experience       NUMERIC(4,1),
    source                 VARCHAR(30) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_candidate_skills PRIMARY KEY (candidate_profile_id, skill_id),
    CONSTRAINT chk_candidate_skills_years CHECK (years_experience IS NULL OR years_experience >= 0),
    CONSTRAINT chk_candidate_skills_source CHECK (
        source IN ('SELF_DECLARED', 'RESUME_PARSED', 'VERIFIED')
    )
);

CREATE INDEX idx_candidate_skills_skill ON candidate_skills (skill_id, candidate_profile_id);

CREATE TABLE resumes (
    id                     UUID PRIMARY KEY,
    candidate_profile_id   UUID NOT NULL REFERENCES candidate_profiles (id) ON DELETE CASCADE,
    storage_provider       VARCHAR(30) NOT NULL,
    storage_key            TEXT NOT NULL,
    original_filename      VARCHAR(255) NOT NULL,
    content_type           VARCHAR(100) NOT NULL,
    file_size_bytes        BIGINT NOT NULL,
    checksum_sha256        VARCHAR(64),
    processing_status      VARCHAR(30) NOT NULL,
    is_primary             BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at           TIMESTAMPTZ,
    CONSTRAINT chk_resumes_processing_status CHECK (
        processing_status IN ('UPLOADED', 'PROCESSING', 'COMPLETED', 'FAILED', 'NEEDS_REVIEW')
    ),
    CONSTRAINT chk_resumes_file_size CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_resumes_candidate ON resumes (candidate_profile_id);
