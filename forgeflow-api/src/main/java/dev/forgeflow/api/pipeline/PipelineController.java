package dev.forgeflow.api.pipeline;

import dev.forgeflow.api.common.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/pipelines")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping
    public List<PipelineResponse> list(
            @PathVariable UUID projectId, @AuthenticationPrincipal AuthenticatedUser principal) {
        return pipelineService.listForProject(projectId, principal.id());
    }

    @GetMapping("/{pipelineId}")
    public PipelineDetailResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return pipelineService.getInProject(projectId, pipelineId, principal.id());
    }

    @GetMapping("/{pipelineId}/log-url")
    public LogUrlResponse logUrl(
            @PathVariable UUID projectId,
            @PathVariable UUID pipelineId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return pipelineService.getLogUrl(projectId, pipelineId, principal.id());
    }
}
