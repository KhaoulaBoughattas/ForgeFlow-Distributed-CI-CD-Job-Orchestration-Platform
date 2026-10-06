package dev.forgeflow.api.project;

import dev.forgeflow.api.organization.MembershipRole;
import dev.forgeflow.api.organization.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationService organizationService;

    public ProjectService(ProjectRepository projectRepository, OrganizationService organizationService) {
        this.projectRepository = projectRepository;
        this.organizationService = organizationService;
    }

    @Transactional
    public ProjectResponse create(UUID organizationId, CreateProjectRequest request, UUID requestingUserId) {
        organizationService.requireRole(organizationId, requestingUserId, MembershipRole.ADMIN);

        if (projectRepository.existsByOrganizationIdAndSlug(organizationId, request.slug())) {
            throw new SlugAlreadyExistsInOrganizationException(request.slug());
        }

        Project project = Project.create(organizationId, request.name().trim(), request.slug().trim());
        projectRepository.save(project);
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID organizationId, UUID requestingUserId) {
        organizationService.requireMembership(organizationId, requestingUserId);
        return projectRepository.findByOrganizationId(organizationId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UUID requestingUserId) {
        Project project = requireProjectAccess(projectId, requestingUserId);
        return ProjectResponse.from(project);
    }

    /** Loads the project and verifies the requesting user is a member of its owning organization. */
    @Transactional(readOnly = true)
    public Project requireProjectAccess(UUID projectId, UUID requestingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        organizationService.requireMembership(project.getOrganizationId(), requestingUserId);
        return project;
    }

    /** Loads the project and verifies the requesting user holds at least minRole in its organization. */
    @Transactional(readOnly = true)
    public Project requireProjectWithRole(UUID projectId, UUID requestingUserId, MembershipRole minRole) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        organizationService.requireRole(project.getOrganizationId(), requestingUserId, minRole);
        return project;
    }
}
