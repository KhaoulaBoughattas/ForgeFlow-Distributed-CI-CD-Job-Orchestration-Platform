-- Denormalize github_owner/github_repo onto pipelines so a pipeline remains fully describable
-- even if its repository is later disconnected, and so PipelineReaper's requeue path can
-- republish a PipelineQueuedEvent without an extra join back to git_repositories.
ALTER TABLE pipelines
    ADD COLUMN github_owner VARCHAR(255),
    ADD COLUMN github_repo  VARCHAR(255);

UPDATE pipelines p
SET github_owner = r.github_owner,
    github_repo  = r.github_repo
FROM git_repositories r
WHERE p.repository_id = r.id;

ALTER TABLE pipelines
    ALTER COLUMN github_owner SET NOT NULL,
    ALTER COLUMN github_repo SET NOT NULL;

-- Idempotency ledger for GitHub webhook deliveries (see WebhookService / WebhookDelivery).
CREATE TABLE webhook_deliveries (
    id                  UUID PRIMARY KEY,
    github_delivery_id  VARCHAR(100) NOT NULL UNIQUE,
    received_at         TIMESTAMPTZ NOT NULL
);
