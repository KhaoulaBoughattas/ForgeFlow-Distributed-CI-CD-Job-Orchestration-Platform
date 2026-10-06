CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(120) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_users_email ON users (email);
