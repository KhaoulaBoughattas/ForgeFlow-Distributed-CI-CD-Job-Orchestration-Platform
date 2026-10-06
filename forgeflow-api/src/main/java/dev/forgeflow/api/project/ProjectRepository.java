package dev.forgeflow.api.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsByOrganizationIdAndSlug(UUID organizationId, String slug);

    List<Project> findByOrganizationId(UUID organizationId);
}
