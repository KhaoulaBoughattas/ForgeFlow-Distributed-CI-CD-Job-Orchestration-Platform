package dev.forgeflow.api.repository;

import dev.forgeflow.api.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping
    public ResponseEntity<ConnectRepositoryResponse> connect(
            @PathVariable UUID projectId,
            @Valid @RequestBody ConnectRepositoryRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repositoryService.connect(projectId, request, principal.id()));
    }

    @GetMapping
    public List<RepositoryResponse> list(
            @PathVariable UUID projectId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return repositoryService.list(projectId, principal.id());
    }

    @DeleteMapping("/{repositoryId}")
    public ResponseEntity<Void> disconnect(
            @PathVariable UUID projectId,
            @PathVariable UUID repositoryId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        repositoryService.disconnect(projectId, repositoryId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
