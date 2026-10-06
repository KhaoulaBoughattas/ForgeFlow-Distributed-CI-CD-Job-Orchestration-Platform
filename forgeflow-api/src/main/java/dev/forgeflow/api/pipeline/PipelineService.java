package dev.forgeflow.api.pipeline;

import dev.forgeflow.api.common.storage.ArtifactStorageService;
import dev.forgeflow.api.pipeline.event.PipelineCreatedEvent;
import dev.forgeflow.api.project.Project;
import dev.forgeflow.api.project.ProjectService;
import dev.forgeflow.api.repository.GitRepository;
import dev.forgeflow.api.repository.GitRepositoryRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final ProjectService projectService;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final ArtifactStorageService artifactStorageService;
    private final ApplicationEventPublisher eventPublisher;

    public PipelineService(
            PipelineRepository pipelineRepository,
            ProjectService projectService,
            GitRepositoryRepository gitRepositoryRepository,
            ArtifactStorageService artifactStorageService,
            ApplicationEventPublisher eventPublisher) {
        this.pipelineRepository = pipelineRepository;
        this.projectService = projectService;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.artifactStorageService = artifactStorageService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Admits a new pipeline run for a repository that belongs to the given project. Called both
     * from PipelineController (manual trigger) and WebhookService (push-triggered). Publishes a
     * {@link PipelineCreatedEvent} that only takes effect (QUEUED transition + Kafka publish)
     * after this transaction commits -- see JobAdmissionListener / PipelineEventPublisher.
     */
    @Transactional
    public PipelineResponse create(GitRepository repository, String commitSha, String branch) {
        Pipeline pipeline = Pipeline.createPending(repository.getId(), repository.getGithubOwner(),
                repository.getGithubRepo(), commitSha, branch);
        pipelineRepository.save(pipeline);

        eventPublisher.publishEvent(new PipelineCreatedEvent(
                pipeline.getId(), repository.getId(), repository.getGithubOwner(), repository.getGithubRepo(),
                commitSha, branch, Instant.now()));

        return PipelineResponse.from(pipeline);
    }

    @Transactional(readOnly = true)
    public List<PipelineResponse> listForProject(UUID projectId, UUID requestingUserId) {
        projectService.requireProjectAccess(projectId, requestingUserId);
        Set<UUID> repositoryIds = repositoryIdsForProject(projectId);
        if (repositoryIds.isEmpty()) {
            return List.of();
        }
        return pipelineRepository.findAll().stream()
                .filter(p -> repositoryIds.contains(p.getRepositoryId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(PipelineResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PipelineDetailResponse getInProject(UUID projectId, UUID pipelineId, UUID requestingUserId) {
        Pipeline pipeline = loadPipelineInProject(projectId, pipelineId, requestingUserId);
        return PipelineDetailResponse.from(pipeline);
    }

    @Transactional(readOnly = true)
    public Pipeline loadPipelineInProject(UUID projectId, UUID pipelineId, UUID requestingUserId) {
        projectService.requireProjectAccess(projectId, requestingUserId);
        Pipeline pipeline = pipelineRepository.findById(pipelineId)
                .orElseThrow(() -> new PipelineNotFoundException(pipelineId));

        if (!repositoryIdsForProject(projectId).contains(pipeline.getRepositoryId())) {
            throw new PipelineNotFoundException(pipelineId);
        }
        return pipeline;
    }

    @Transactional(readOnly = true)
    public LogUrlResponse getLogUrl(UUID projectId, UUID pipelineId, UUID requestingUserId) {
        Pipeline pipeline = loadPipelineInProject(projectId, pipelineId, requestingUserId);
        if (pipeline.getLogObjectKey() == null) {
            throw new LogNotAvailableException(pipelineId);
        }
        String url = artifactStorageService.presignDownloadUrl(pipeline.getLogObjectKey()).toString();
        return new LogUrlResponse(url, 15 * 60);
    }

    private Set<UUID> repositoryIdsForProject(UUID projectId) {
        return gitRepositoryRepository.findByProjectId(projectId).stream()
                .map(GitRepository::getId)
                .collect(Collectors.toSet());
    }
}
