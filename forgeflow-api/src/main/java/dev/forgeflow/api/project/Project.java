package dev.forgeflow.api.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "slug"}))
public class Project implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected Project() {
        // JPA
    }

    private Project(UUID id, UUID organizationId, String name, String slug, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.slug = slug;
        this.createdAt = createdAt;
    }

    public static Project create(UUID organizationId, String name, String slug) {
        return new Project(UUID.randomUUID(), organizationId, name, slug, Instant.now());
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
