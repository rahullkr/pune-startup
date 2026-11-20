CREATE TABLE company_locations (
    id                UUID PRIMARY KEY,
    company_id        UUID NOT NULL REFERENCES companies (id),
    location_name     VARCHAR(150),
    address_line      TEXT,
    area              VARCHAR(120),
    city              VARCHAR(100) NOT NULL,
    state             VARCHAR(100) NOT NULL,
    country_code      CHAR(2) NOT NULL,
    postal_code       VARCHAR(15),
    location_type     VARCHAR(30) NOT NULL,
    location          GEOGRAPHY(POINT, 4326),
    geocode_status    VARCHAR(30) NOT NULL,
    is_headquarters   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_company_locations_location_type CHECK (
        location_type IN ('HEADQUARTERS', 'OFFICE', 'COWORKING', 'OTHER')
    ),
    CONSTRAINT chk_company_locations_geocode_status CHECK (
        geocode_status IN ('PENDING', 'VERIFIED', 'APPROXIMATE', 'FAILED')
    ),
    CONSTRAINT chk_company_locations_country_code CHECK (country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_company_locations_company ON company_locations (company_id);
CREATE INDEX idx_company_locations_location ON company_locations USING GIST (location);
CREATE INDEX idx_company_locations_area ON company_locations (area);
