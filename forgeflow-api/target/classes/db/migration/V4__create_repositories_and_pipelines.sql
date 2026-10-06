CREATE TABLE git_repositories (
    id                      UUID PRIMARY KEY,
    project_id              UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    github_owner            VARCHAR(255) NOT NULL,
    github_repo             VARCHAR(255) NOT NULL,
    webhook_secret_hash     VARCHAR(255) NOT NULL,
    connected               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_git_repositories_project ON git_repositories (project_id);
CREATE INDEX idx_git_repositories_owner_repo ON git_repositories (github_owner, github_repo);

CREATE TABLE pipelines (
    id              UUID PRIMARY KEY,
    repository_id   UUID NOT NULL REFERENCES git_repositories (id) ON DELETE CASCADE,
    commit_sha      VARCHAR(40) NOT NULL,
    branch          VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_pipelines_repository ON pipelines (repository_id);
CREATE INDEX idx_pipelines_status ON pipelines (status);
