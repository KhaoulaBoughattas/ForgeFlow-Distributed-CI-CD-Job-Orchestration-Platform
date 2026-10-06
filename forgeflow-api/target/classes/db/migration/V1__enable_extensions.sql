-- pgcrypto is used nowhere directly by application SQL (UUIDs are generated in application
-- code), but is enabled as a convenience for ad-hoc operational queries against this schema.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
