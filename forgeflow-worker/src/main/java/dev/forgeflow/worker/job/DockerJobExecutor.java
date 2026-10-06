package dev.forgeflow.worker.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Runs each configured pipeline step as its own `docker run` of the step's image, mounting the
 * cloned workspace read-write. Resource limits (--memory, --cpus) and a non-root user are
 * applied to every step so an untrusted repository's build script cannot exhaust the host or
 * write files the worker process can't clean up. If a step exceeds the configured timeout, we
 * first try `docker stop` (graceful SIGTERM) and fall back to destroyForcibly if the container
 * still hasn't exited.
 */
@Component
public class DockerJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(DockerJobExecutor.class);
    private static final int LOG_TAIL_LINES = 100;

    private final GitWorkspaceCloner workspaceCloner;
    private final WorkspaceConfigReader configReader;
    private final PipelineConfigParser configParser;
    private final long timeoutSeconds;
    private final String memoryLimit;
    private final String cpuLimit;

    public DockerJobExecutor(
            GitWorkspaceCloner workspaceCloner,
            WorkspaceConfigReader configReader,
            PipelineConfigParser configParser,
            @Value("${forgeflow.job.timeout-seconds:900}") long timeoutSeconds,
            @Value("${forgeflow.job.memory-limit:512m}") String memoryLimit,
            @Value("${forgeflow.job.cpu-limit:1.0}") String cpuLimit) {
        this.workspaceCloner = workspaceCloner;
        this.configReader = configReader;
        this.configParser = configParser;
        this.timeoutSeconds = timeoutSeconds;
        this.memoryLimit = memoryLimit;
        this.cpuLimit = cpuLimit;
    }

    public record JobResult(boolean succeeded, int exitCode, String logTail, String fullLog) {
    }

    public JobResult execute(UUID pipelineId, String githubOwner, String githubRepo, String commitSha) {
        StringBuilder fullLog = new StringBuilder();
        Path workspace = workspaceCloner.clone(githubOwner, githubRepo, commitSha);
        fullLog.append("Cloned ").append(githubOwner).append('/').append(githubRepo)
                .append('@').append(commitSha).append(" into ").append(workspace).append('\n');

        PipelineConfigParser.PipelineConfig config;
        try {
            config = configParser.parse(configReader.readConfig(workspace));
        } catch (PipelineConfigException ex) {
            fullLog.append("Pipeline configuration error: ").append(ex.getMessage()).append('\n');
            return new JobResult(false, 1, tail(fullLog.toString()), fullLog.toString());
        }

        int exitCode = 0;
        for (PipelineConfigParser.PipelineStep step : config.steps()) {
            fullLog.append("\n=== Step: ").append(step.name()).append(" ===\n");
            StepResult result = runStep(pipelineId, workspace, config.image(), step.run());
            fullLog.append(result.output());

            if (result.exitCode() != 0) {
                exitCode = result.exitCode();
                fullLog.append("\nStep '").append(step.name()).append("' failed with exit code ").append(exitCode).append('\n');
                break;
            }
        }

        boolean succeeded = exitCode == 0;
        cleanupWorkspace(workspace);
        return new JobResult(succeeded, exitCode, tail(fullLog.toString()), fullLog.toString());
    }

    private record StepResult(int exitCode, String output) {
    }

    private StepResult runStep(UUID pipelineId, Path workspace, String image, String runCommand) {
        List<String> command = new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "--name", "forgeflow-" + pipelineId,
                "--memory", memoryLimit,
                "--cpus", cpuLimit,
                "--user", "1000:1000",
                "-v", workspace + ":/workspace",
                "-w", "/workspace",
                image,
                "sh", "-c", runCommand));

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes());

            if (!finished) {
                log.warn("Step exceeded timeout of {}s, stopping container forgeflow-{}", timeoutSeconds, pipelineId);
                stopContainer(pipelineId);
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
                return new StepResult(124, output + "\n[step timed out after " + timeoutSeconds + "s]\n");
            }
            return new StepResult(process.exitValue(), output);
        } catch (Exception ex) {
            throw new JobExecutionException("Failed to run step via docker", ex);
        }
    }

    private void stopContainer(UUID pipelineId) {
        try {
            new ProcessBuilder("docker", "stop", "forgeflow-" + pipelineId).start().waitFor(15, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("Failed to gracefully stop container for pipeline {}", pipelineId, ex);
        }
    }

    private void cleanupWorkspace(Path workspace) {
        try {
            try (var paths = java.nio.file.Files.walk(workspace)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                java.nio.file.Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                                // Best-effort cleanup; a leftover temp dir is not fatal.
                            }
                        });
            }
        } catch (Exception ex) {
            log.warn("Could not fully clean up workspace {}", workspace, ex);
        }
    }

    private String tail(String log) {
        String[] lines = log.split("\n");
        int from = Math.max(0, lines.length - LOG_TAIL_LINES);
        return String.join("\n", java.util.Arrays.asList(lines).subList(from, lines.length));
    }
}
