ALTER TABLE topic_progress
    DROP CONSTRAINT IF EXISTS chk_topic_state;

ALTER TABLE topic_progress
    ADD CONSTRAINT chk_topic_state CHECK (state IN ('LOCKED', 'AVAILABLE', 'VISITED', 'COMPLETED'));

CREATE TABLE exercise_attempts (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id            VARCHAR(50)     NOT NULL,
    exercise_id         VARCHAR(80)     NOT NULL,
    type                VARCHAR(24)     NOT NULL,
    answer              TEXT            NOT NULL,
    correct             BOOLEAN         NOT NULL,
    time_spent_seconds  INTEGER,
    attempted_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exercise_attempts_user ON exercise_attempts(user_id);
CREATE INDEX idx_exercise_attempts_user_topic ON exercise_attempts(user_id, topic_id);

CREATE TABLE user_badges (
    id           BIGSERIAL       PRIMARY KEY,
    user_id      BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id     VARCHAR(60)     NOT NULL,
    name         VARCHAR(100)    NOT NULL,
    description  VARCHAR(255)    NOT NULL,
    earned_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_badge UNIQUE (user_id, badge_id)
);
