package dev.forgeflow.api.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "organization_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "user_id"}))
public class OrganizationMembership implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected OrganizationMembership() {
        // JPA
    }

    private OrganizationMembership(UUID id, UUID organizationId, UUID userId, MembershipRole role, Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static OrganizationMembership create(UUID organizationId, UUID userId, MembershipRole role) {
        return new OrganizationMembership(UUID.randomUUID(), organizationId, userId, role, Instant.now());
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

    public UUID getUserId() {
        return userId;
    }

    public MembershipRole getRole() {
        return role;
    }

    public void setRole(MembershipRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
