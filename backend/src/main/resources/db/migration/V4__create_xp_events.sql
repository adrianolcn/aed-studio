-- ═══════════════════════════════════════════════════════
--  V4 — Log de eventos XP (evita double-awarding)
--  Cada (user_id, event_key) é único — idempotente.
-- ═══════════════════════════════════════════════════════

CREATE TABLE xp_events (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    event_key   VARCHAR(100)    NOT NULL,
    amount      INTEGER         NOT NULL,
    earned_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_event UNIQUE (user_id, event_key),
    CONSTRAINT chk_amount    CHECK (amount > 0)
);

CREATE INDEX idx_xp_events_user ON xp_events(user_id);
