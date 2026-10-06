package dev.forgeflow.worker.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Clones a repository at a specific commit into a fresh temporary workspace directory, using the
 * official `alpine/git` image via `docker run` rather than a git binary installed on the worker
 * host itself -- the worker's own runtime image stays minimal, and every clone runs in the same
 * pinned, disposable environment regardless of what's installed on the host.
 */
@Component
public class GitWorkspaceCloner {

    private static final Logger log = LoggerFactory.getLogger(GitWorkspaceCloner.class);
    private static final String GIT_IMAGE = "alpine/git:2.47.1";
    private static final Duration CLONE_TIMEOUT = Duration.ofMinutes(5);

    public Path clone(String githubOwner, String githubRepo, String commitSha) {
        Path workspace;
        try {
            workspace = Files.createTempDirectory("forgeflow-job-");
        } catch (Exception ex) {
            throw new JobExecutionException("Could not create a temporary workspace directory", ex);
        }

        String repoUrl = "https://github.com/" + githubOwner + "/" + githubRepo + ".git";

        run(workspace, List.of("docker", "run", "--rm",
                "-v", workspace + ":/workspace",
                "-w", "/workspace",
                GIT_IMAGE, "clone", "--no-checkout", repoUrl, "."));

        run(workspace, List.of("docker", "run", "--rm",
                "-v", workspace + ":/workspace",
                "-w", "/workspace",
                GIT_IMAGE, "checkout", commitSha));

        return workspace;
    }

    private void run(Path workspace, List<String> command) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(CLONE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes());
            if (!finished) {
                process.destroyForcibly();
                throw new JobExecutionException("git clone/checkout timed out after " + CLONE_TIMEOUT);
            }
            if (process.exitValue() != 0) {
                log.warn("git command failed for workspace {}: {}", workspace, output);
                throw new JobExecutionException("git command failed with exit code " + process.exitValue() + ": " + output);
            }
        } catch (JobExecutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JobExecutionException("Failed to run git command via docker", ex);
        }
    }
}
