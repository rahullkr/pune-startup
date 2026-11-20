CREATE TABLE auth_sessions (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash    VARCHAR(64) NOT NULL,
    user_agent    VARCHAR(255),
    expires_at    TIMESTAMPTZ NOT NULL,
    revoked_at    TIMESTAMPTZ,
    last_used_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_auth_sessions_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_auth_sessions_user_active ON auth_sessions (user_id, revoked_at);
CREATE INDEX idx_auth_sessions_expires ON auth_sessions (expires_at);
