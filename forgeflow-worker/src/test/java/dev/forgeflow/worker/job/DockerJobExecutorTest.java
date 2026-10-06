package dev.forgeflow.worker.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DockerJobExecutor's control flow (config-error short-circuit, step failure
 * stopping the pipeline). These stub out GitWorkspaceCloner so no real `docker run` is invoked;
 * genuinely exercising container execution is left to a manual/integration smoke test against a
 * live Docker daemon, which is outside the scope of this fast unit suite.
 */
class DockerJobExecutorTest {

    private final GitWorkspaceCloner cloner = mock(GitWorkspaceCloner.class);
    private final WorkspaceConfigReader configReader = mock(WorkspaceConfigReader.class);
    private final PipelineConfigParser configParser = mock(PipelineConfigParser.class);

    private final DockerJobExecutor executor =
            new DockerJobExecutor(cloner, configReader, configParser, 30L, "256m", "0.5");

    @Test
    void returnsFailureWhenPipelineConfigIsInvalid(@TempDir Path workspace) {
        UUID pipelineId = UUID.randomUUID();
        when(cloner.clone("acme", "widgets", "abc123")).thenReturn(workspace);
        when(configReader.readConfig(workspace)).thenReturn("not valid");
        when(configParser.parse("not valid"))
                .thenThrow(new PipelineConfigException("bad config: missing image"));

        DockerJobExecutor.JobResult result = executor.execute(pipelineId, "acme", "widgets", "abc123");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.fullLog()).contains("Pipeline configuration error");
        assertThat(result.fullLog()).contains("bad config: missing image");
        assertThat(result.logTail()).contains("bad config: missing image");
    }

    @Test
    void logsClonedRepositoryDetailsEvenOnConfigFailure(@TempDir Path workspace) {
        UUID pipelineId = UUID.randomUUID();
        when(cloner.clone("acme", "widgets", "deadbeef")).thenReturn(workspace);
        when(configReader.readConfig(workspace)).thenReturn("");
        when(configParser.parse(any())).thenThrow(new PipelineConfigException("no image"));

        DockerJobExecutor.JobResult result = executor.execute(pipelineId, "acme", "widgets", "deadbeef");

        assertThat(result.fullLog()).contains("Cloned acme/widgets@deadbeef");
    }

    @Test
    void propagatesCloneFailureAsJobExecutionException(@TempDir Path workspace) {
        when(cloner.clone(any(), any(), any()))
                .thenThrow(new JobExecutionException("git command failed with exit code 128"));

        org.junit.jupiter.api.Assertions.assertThrows(JobExecutionException.class,
                () -> executor.execute(UUID.randomUUID(), "acme", "widgets", "abc123"));
    }

    @Test
    void jobResultRecordExposesAllFields() {
        DockerJobExecutor.JobResult result = new DockerJobExecutor.JobResult(true, 0, "tail", "full log here");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.exitCode()).isZero();
        assertThat(result.logTail()).isEqualTo("tail");
        assertThat(result.fullLog()).isEqualTo("full log here");
    }

    @Test
    void cleansUpWorkspaceDirectoryAfterConfigFailure(@TempDir Path tempRoot) throws Exception {
        Path workspace = Files.createTempDirectory(tempRoot, "forgeflow-job-");
        Files.writeString(workspace.resolve("file.txt"), "contents");
        UUID pipelineId = UUID.randomUUID();
        when(cloner.clone("acme", "widgets", "abc123")).thenReturn(workspace);
        when(configReader.readConfig(workspace)).thenReturn("bad");
        when(configParser.parse("bad")).thenThrow(new PipelineConfigException("bad"));

        executor.execute(pipelineId, "acme", "widgets", "abc123");

        assertThat(Files.exists(workspace)).isFalse();
    }
}
