package dev.forgeflow.api.project;

import dev.forgeflow.api.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(organizationId, request, principal.id()));
    }

    @GetMapping
    public List<ProjectResponse> list(
            @PathVariable UUID organizationId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return projectService.list(organizationId, principal.id());
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(
            @PathVariable UUID organizationId,
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return projectService.get(projectId, principal.id());
    }
}
