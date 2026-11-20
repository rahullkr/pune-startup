ALTER TABLE scraping_sources
    ADD COLUMN permission_notes TEXT;

ALTER TABLE scraping_sources
    ADD CONSTRAINT chk_scraping_sources_type CHECK (
        source_type IN ('GREENHOUSE', 'LEVER', 'ASHBY', 'COMPANY_CAREERS', 'PARTNER_FEED', 'OTHER')
    );

CREATE INDEX idx_scraping_sources_enabled ON scraping_sources (is_enabled, permission_review_status);
