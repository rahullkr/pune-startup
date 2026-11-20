ALTER TABLE companies
    ADD COLUMN website_domain VARCHAR(253);

UPDATE companies
SET website_domain = NULLIF(
    regexp_replace(
        regexp_replace(
            regexp_replace(lower(website_url), '^https?://', ''),
            '^www\.', ''),
        '[:/?#].*$', ''),
    '')
WHERE website_url IS NOT NULL;

CREATE INDEX idx_companies_website_domain ON companies (website_domain);

ALTER TABLE companies DROP CONSTRAINT chk_companies_record_status;

ALTER TABLE companies
    ADD CONSTRAINT chk_companies_record_status CHECK (
        record_status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED', 'MERGED')
    );
