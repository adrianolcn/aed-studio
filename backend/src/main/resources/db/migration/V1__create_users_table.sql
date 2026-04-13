-- ═══════════════════════════════════════════════════════
--  V1 — Tabela de usuários
-- ═══════════════════════════════════════════════════════

CREATE TABLE users (
    id                  BIGSERIAL       PRIMARY KEY,
    username            VARCHAR(50)     NOT NULL UNIQUE,
    email               VARCHAR(100)    NOT NULL UNIQUE,
    password            VARCHAR(255)    NOT NULL,
    full_name           VARCHAR(100)    NOT NULL,
    role                VARCHAR(20)     NOT NULL DEFAULT 'STUDENT',
    xp                  INTEGER         NOT NULL DEFAULT 0,
    streak_days         INTEGER         NOT NULL DEFAULT 0,
    last_study_date     DATE,
    topics_completed    INTEGER         NOT NULL DEFAULT 0,
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    account_non_locked  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,

    CONSTRAINT chk_role CHECK (role IN ('STUDENT', 'ADMIN')),
    CONSTRAINT chk_xp   CHECK (xp >= 0)
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);
