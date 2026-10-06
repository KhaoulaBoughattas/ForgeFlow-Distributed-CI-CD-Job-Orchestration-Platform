package dev.forgeflow.api.repository;

import dev.forgeflow.api.common.security.WebhookSecretCipher;
import dev.forgeflow.api.organization.MembershipRole;
import dev.forgeflow.api.project.Project;
import dev.forgeflow.api.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class RepositoryService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GitRepositoryRepository repositoryRepository;
    private final ProjectService projectService;
    private final WebhookSecretCipher webhookSecretCipher;

    public RepositoryService(
            GitRepositoryRepository repositoryRepository,
            ProjectService projectService,
            WebhookSecretCipher webhookSecretCipher) {
        this.repositoryRepository = repositoryRepository;
        this.projectService = projectService;
        this.webhookSecretCipher = webhookSecretCipher;
    }

    @Transactional
    public ConnectRepositoryResponse connect(UUID projectId, ConnectRepositoryRequest request, UUID requestingUserId) {
        Project project = projectService.requireProjectWithRole(projectId, requestingUserId, MembershipRole.ADMIN);

        if (repositoryRepository.existsByProjectIdAndGithubOwnerAndGithubRepoAndConnectedTrue(
                projectId, request.githubOwner(), request.githubRepo())) {
            throw new RepositoryAlreadyConnectedException(request.githubOwner(), request.githubRepo());
        }

        String plaintextSecret = generateWebhookSecret();
        GitRepository repository = GitRepository.connect(
                project.getId(), request.githubOwner(), request.githubRepo(), webhookSecretCipher.encrypt(plaintextSecret));
        repositoryRepository.save(repository);

        return new ConnectRepositoryResponse(RepositoryResponse.from(repository), plaintextSecret);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> list(UUID projectId, UUID requestingUserId) {
        projectService.requireProjectAccess(projectId, requestingUserId);
        return repositoryRepository.findByProjectId(projectId).stream()
                .map(RepositoryResponse::from)
                .toList();
    }

    @Transactional
    public void disconnect(UUID projectId, UUID repositoryId, UUID requestingUserId) {
        projectService.requireProjectWithRole(projectId, requestingUserId, MembershipRole.ADMIN);
        GitRepository repository = repositoryRepository.findById(repositoryId)
                .filter(r -> r.getProjectId().equals(projectId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        repository.disconnect();
        repositoryRepository.save(repository);
    }

    /** Looks up the single connected repository matching an incoming webhook's owner/repo. */
    @Transactional(readOnly = true)
    public GitRepository requireConnectedRepository(String githubOwner, String githubRepo) {
        return repositoryRepository.findByGithubOwnerAndGithubRepoAndConnectedTrue(githubOwner, githubRepo)
                .orElseThrow(() -> new RepositoryNotConnectedException(githubOwner, githubRepo));
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
