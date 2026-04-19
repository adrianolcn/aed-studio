CREATE TABLE code_submissions (
    id                 BIGSERIAL       PRIMARY KEY,
    user_id            BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id           VARCHAR(50)     NOT NULL,
    exercise_id        VARCHAR(90)     NOT NULL,
    source_code        TEXT            NOT NULL,
    status             VARCHAR(20)     NOT NULL,
    total_tests        INTEGER         NOT NULL DEFAULT 0,
    passed_tests       INTEGER         NOT NULL DEFAULT 0,
    execution_time_ms  BIGINT          NOT NULL DEFAULT 0,
    passed_checks      TEXT,
    failed_checks      TEXT,
    feedback           TEXT,
    created_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_code_submissions_user ON code_submissions(user_id);
CREATE INDEX idx_code_submissions_user_topic ON code_submissions(user_id, topic_id);
CREATE INDEX idx_code_submissions_user_exercise ON code_submissions(user_id, exercise_id);
