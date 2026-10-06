CREATE TABLE organizations (
    id          UUID PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(60) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE organization_memberships (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_membership_org_user UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_memberships_user ON organization_memberships (user_id);
CREATE INDEX idx_memberships_org ON organization_memberships (organization_id);

CREATE TABLE projects (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    slug            VARCHAR(60) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_project_org_slug UNIQUE (organization_id, slug)
);

CREATE INDEX idx_projects_organization ON projects (organization_id);
