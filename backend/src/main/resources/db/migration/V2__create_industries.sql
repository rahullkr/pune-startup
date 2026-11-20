CREATE TABLE industries (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(120) NOT NULL,
    description   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_industries_name UNIQUE (name),
    CONSTRAINT uq_industries_slug UNIQUE (slug)
);
