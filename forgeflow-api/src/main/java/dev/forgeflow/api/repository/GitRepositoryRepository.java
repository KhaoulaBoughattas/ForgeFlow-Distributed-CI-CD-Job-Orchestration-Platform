package dev.forgeflow.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GitRepositoryRepository extends JpaRepository<GitRepository, UUID> {
    List<GitRepository> findByProjectId(UUID projectId);

    Optional<GitRepository> findByGithubOwnerAndGithubRepoAndConnectedTrue(String githubOwner, String githubRepo);

    boolean existsByProjectIdAndGithubOwnerAndGithubRepoAndConnectedTrue(UUID projectId, String githubOwner, String githubRepo);
}
