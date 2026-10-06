package dev.forgeflow.worker.job;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads the .forgeflow.yml that {@link GitWorkspaceCloner} checked out into a job's workspace. */
@Component
public class WorkspaceConfigReader {

    private static final String CONFIG_FILE_NAME = ".forgeflow.yml";

    public String readConfig(Path workspace) {
        Path configPath = workspace.resolve(CONFIG_FILE_NAME);
        if (!Files.isRegularFile(configPath)) {
            throw new PipelineConfigException("Repository is missing a " + CONFIG_FILE_NAME + " at its root");
        }
        try {
            return Files.readString(configPath);
        } catch (IOException ex) {
            throw new JobExecutionException("Could not read " + CONFIG_FILE_NAME, ex);
        }
    }
}
