CREATE TABLE company_sources (
    id                 UUID PRIMARY KEY,
    company_id         UUID NOT NULL REFERENCES companies (id),
    source_name        VARCHAR(100) NOT NULL,
    source_url         TEXT NOT NULL,
    external_id        VARCHAR(255),
    last_verified_at   TIMESTAMPTZ,
    source_metadata    JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_company_sources_company ON company_sources (company_id);
CREATE INDEX idx_company_sources_origin ON company_sources (source_name, external_id);
