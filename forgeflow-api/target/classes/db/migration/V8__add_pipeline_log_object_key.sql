ALTER TABLE pipelines
    ADD COLUMN log_object_key VARCHAR(512);

-- Rename to reflect that the webhook secret is now stored encrypted (reversible), not hashed
-- (one-way) -- see WebhookSecretCipher / GitRepository javadoc. Existing rows are invalidated
-- (NULL ciphertext) since a BCrypt hash cannot be converted into ciphertext; operators must
-- reconnect any repositories created before this migration.
ALTER TABLE git_repositories
    RENAME COLUMN webhook_secret_hash TO webhook_secret_ciphertext;

ALTER TABLE git_repositories
    ALTER COLUMN webhook_secret_ciphertext DROP NOT NULL;

UPDATE git_repositories SET webhook_secret_ciphertext = NULL;
