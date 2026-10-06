ALTER TABLE pipelines
    ADD COLUMN worker_id      VARCHAR(80),
    ADD COLUMN started_at     TIMESTAMPTZ,
    ADD COLUMN finished_at    TIMESTAMPTZ,
    ADD COLUMN exit_code      INTEGER,
    ADD COLUMN log_tail       TEXT,
    ADD COLUMN heartbeat_at   TIMESTAMPTZ;
