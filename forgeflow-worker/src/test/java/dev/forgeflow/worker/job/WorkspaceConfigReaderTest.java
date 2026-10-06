package dev.forgeflow.worker.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceConfigReaderTest {

    private final WorkspaceConfigReader reader = new WorkspaceConfigReader();

    @Test
    void readsConfigFileContent(@TempDir Path workspace) throws Exception {
        String content = "image: alpine:3.20\nsteps:\n  - name: build\n    run: echo hi\n";
        Files.writeString(workspace.resolve(".forgeflow.yml"), content);

        String result = reader.readConfig(workspace);

        assertThat(result).isEqualTo(content);
    }

    @Test
    void throwsPipelineConfigExceptionWhenConfigMissing(@TempDir Path workspace) {
        assertThatThrownBy(() -> reader.readConfig(workspace))
                .isInstanceOf(PipelineConfigException.class)
                .hasMessageContaining(".forgeflow.yml");
    }

    @Test
    void throwsPipelineConfigExceptionWhenConfigIsADirectory(@TempDir Path workspace) throws Exception {
        Files.createDirectory(workspace.resolve(".forgeflow.yml"));

        assertThatThrownBy(() -> reader.readConfig(workspace))
                .isInstanceOf(PipelineConfigException.class);
    }

    @Test
    void readsEmptyConfigFile(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve(".forgeflow.yml"), "");

        String result = reader.readConfig(workspace);

        assertThat(result).isEmpty();
    }
}
