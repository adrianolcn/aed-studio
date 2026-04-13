-- ═══════════════════════════════════════════════════════
--  V3 — Progresso por tópico
-- ═══════════════════════════════════════════════════════

CREATE TABLE topic_progress (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id            VARCHAR(50)     NOT NULL,
    state               VARCHAR(20)     NOT NULL DEFAULT 'VISITED',
    xp_earned           INTEGER         NOT NULL DEFAULT 0,
    first_visited_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,

    CONSTRAINT uq_user_topic        UNIQUE (user_id, topic_id),
    CONSTRAINT chk_topic_state      CHECK (state IN ('VISITED', 'COMPLETED')),
    CONSTRAINT chk_xp_earned        CHECK (xp_earned >= 0)
);

CREATE INDEX idx_topic_progress_user ON topic_progress(user_id);
