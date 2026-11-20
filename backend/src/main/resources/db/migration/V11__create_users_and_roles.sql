CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    email               VARCHAR(320) NOT NULL,
    normalized_email    VARCHAR(320) NOT NULL,
    password_hash       TEXT,
    account_status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_normalized_email UNIQUE (normalized_email),
    CONSTRAINT chk_users_account_status CHECK (
        account_status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED')
    )
);

CREATE TABLE user_roles (
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role         VARCHAR(30) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT chk_user_roles_role CHECK (
        role IN ('CANDIDATE', 'RECRUITER', 'ADMIN')
    )
);
