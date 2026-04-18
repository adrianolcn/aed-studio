CREATE TABLE generated_exercises (
    id                BIGSERIAL       PRIMARY KEY,
    user_id           BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    generated_id      VARCHAR(90)     NOT NULL,
    topic_id          VARCHAR(50)     NOT NULL,
    type              VARCHAR(24)     NOT NULL,
    difficulty        INTEGER         NOT NULL DEFAULT 1,
    prompt            TEXT            NOT NULL,
    options           TEXT,
    correct_answer    TEXT            NOT NULL,
    correct_feedback  TEXT            NOT NULL,
    wrong_feedback    TEXT            NOT NULL,
    answered          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_generated_exercise UNIQUE (generated_id)
);

CREATE INDEX idx_generated_exercises_user ON generated_exercises(user_id);
CREATE INDEX idx_generated_exercises_user_topic ON generated_exercises(user_id, topic_id);

CREATE TABLE simulation_events (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id        VARCHAR(50)     NOT NULL,
    simulator_type  VARCHAR(30)     NOT NULL,
    action          VARCHAR(40)     NOT NULL,
    milestone       VARCHAR(60),
    state_snapshot  TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_simulation_events_user ON simulation_events(user_id);
CREATE INDEX idx_simulation_events_user_topic ON simulation_events(user_id, topic_id);
