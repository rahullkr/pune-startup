CREATE TABLE skills (
    id                UUID PRIMARY KEY,
    name              VARCHAR(120) NOT NULL,
    normalized_name   VARCHAR(120) NOT NULL,
    category          VARCHAR(80),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_skills_normalized_name UNIQUE (normalized_name)
);

CREATE TABLE skill_aliases (
    id                 UUID PRIMARY KEY,
    skill_id           UUID NOT NULL REFERENCES skills (id),
    alias              VARCHAR(120) NOT NULL,
    normalized_alias   VARCHAR(120) NOT NULL,
    CONSTRAINT uq_skill_aliases_normalized_alias UNIQUE (normalized_alias)
);

CREATE INDEX idx_skill_aliases_skill ON skill_aliases (skill_id);
