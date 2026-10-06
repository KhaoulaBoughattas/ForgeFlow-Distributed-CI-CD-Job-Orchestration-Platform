package dev.forgeflow.api.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    List<OrganizationMembership> findByOrganizationId(UUID organizationId);

    List<OrganizationMembership> findByUserId(UUID userId);

    long countByOrganizationIdAndRole(UUID organizationId, MembershipRole role);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
