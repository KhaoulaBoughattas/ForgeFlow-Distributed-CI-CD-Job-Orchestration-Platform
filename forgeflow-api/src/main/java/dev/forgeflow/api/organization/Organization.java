package dev.forgeflow.api.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected Organization() {
        // JPA
    }

    private Organization(UUID id, String name, String slug, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.createdAt = createdAt;
    }

    public static Organization create(String name, String slug) {
        return new Organization(UUID.randomUUID(), name, slug, Instant.now());
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
