CREATE TABLE notifications (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    notification_type   VARCHAR(50) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    body               TEXT,
    payload            JSONB,
    read_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    actor_user_id   UUID REFERENCES users (id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(80) NOT NULL,
    entity_id       UUID,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id, created_at DESC);
