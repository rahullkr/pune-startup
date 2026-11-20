CREATE TABLE companies (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(200) NOT NULL,
    normalized_name       VARCHAR(220) NOT NULL,
    slug                  VARCHAR(220) NOT NULL,
    website_url           TEXT,
    logo_url              TEXT,
    description           TEXT,
    industry_id           UUID REFERENCES industries (id),
    founded_year          SMALLINT,
    employee_count_min    INTEGER,
    employee_count_max    INTEGER,
    funding_stage         VARCHAR(50),
    company_type          VARCHAR(40) NOT NULL DEFAULT 'STARTUP',
    verification_status   VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    record_status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_companies_slug UNIQUE (slug),
    CONSTRAINT chk_companies_founded_year CHECK (
        founded_year IS NULL OR (founded_year >= 1800 AND founded_year <= EXTRACT(YEAR FROM now()))
    ),
    CONSTRAINT chk_companies_employee_min CHECK (employee_count_min IS NULL OR employee_count_min >= 0),
    CONSTRAINT chk_companies_employee_max CHECK (employee_count_max IS NULL OR employee_count_max >= 0),
    CONSTRAINT chk_companies_employee_range CHECK (
        employee_count_min IS NULL OR employee_count_max IS NULL OR employee_count_min <= employee_count_max
    ),
    CONSTRAINT chk_companies_company_type CHECK (
        company_type IN ('STARTUP', 'SME', 'ENTERPRISE', 'AGENCY', 'ACCELERATOR', 'OTHER')
    ),
    CONSTRAINT chk_companies_verification_status CHECK (
        verification_status IN ('UNVERIFIED', 'PENDING_REVIEW', 'VERIFIED', 'REJECTED')
    ),
    CONSTRAINT chk_companies_record_status CHECK (
        record_status IN ('ACTIVE', 'INACTIVE', 'MERGED')
    )
);

CREATE INDEX idx_companies_normalized_name ON companies (normalized_name);
CREATE INDEX idx_companies_industry ON companies (industry_id);
CREATE INDEX idx_companies_record_status ON companies (record_status);
