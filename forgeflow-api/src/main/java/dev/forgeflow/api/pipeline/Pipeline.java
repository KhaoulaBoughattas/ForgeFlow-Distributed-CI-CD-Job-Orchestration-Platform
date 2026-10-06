package dev.forgeflow.api.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A single run of a repository's pipeline against one commit. Mutating transitions are exposed
 * as named methods (markQueued, markRunning, ...) rather than plain setters, so the valid state
 * machine lives in one place instead of being reconstructed at every call site.
 */
@Entity
@Table(name = "pipelines")
public class Pipeline implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "github_owner", nullable = false)
    private String githubOwner;

    @Column(name = "github_repo", nullable = false)
    private String githubRepo;

    @Column(name = "commit_sha", nullable = false)
    private String commitSha;

    @Column(nullable = false)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PipelineStatus status;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Lob
    @Column(name = "log_tail")
    private String logTail;

    @Column(name = "log_object_key")
    private String logObjectKey;

    @Column(name = "heartbeat_at")
    private Instant heartbeatAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    protected Pipeline() {
        // JPA
    }

    private Pipeline(UUID id, UUID repositoryId, String githubOwner, String githubRepo,
                      String commitSha, String branch, PipelineStatus status, Instant createdAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
        this.commitSha = commitSha;
        this.branch = branch;
        this.status = status;
        this.retryCount = 0;
        this.createdAt = createdAt;
    }

    public static Pipeline createPending(UUID repositoryId, String githubOwner, String githubRepo,
                                          String commitSha, String branch) {
        return new Pipeline(UUID.randomUUID(), repositoryId, githubOwner, githubRepo, commitSha, branch,
                PipelineStatus.PENDING, Instant.now());
    }

    public void markQueued() {
        this.status = PipelineStatus.QUEUED;
    }

    public void markRunning(String workerId) {
        this.status = PipelineStatus.RUNNING;
        this.workerId = workerId;
        this.startedAt = Instant.now();
        this.heartbeatAt = Instant.now();
    }

    public void markFinished(boolean succeeded, int exitCode, String logTail, String logObjectKey) {
        this.status = succeeded ? PipelineStatus.SUCCEEDED : PipelineStatus.FAILED;
        this.exitCode = exitCode;
        this.logTail = logTail;
        this.logObjectKey = logObjectKey;
        this.finishedAt = Instant.now();
    }

    public void recordLogObjectKey(String logObjectKey) {
        this.logObjectKey = logObjectKey;
    }

    public void recordHeartbeat() {
        this.heartbeatAt = Instant.now();
    }

    public void scheduleRetry(Duration delay) {
        this.status = PipelineStatus.RETRY_SCHEDULED;
        this.retryCount += 1;
        this.nextRetryAt = Instant.now().plus(delay);
    }

    public void requeueAfterRetryDelay() {
        this.status = PipelineStatus.QUEUED;
        this.workerId = null;
        this.heartbeatAt = null;
    }

    public void markFailedExhausted() {
        this.status = PipelineStatus.FAILED;
        this.finishedAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getRepositoryId() {
        return repositoryId;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public String getGithubRepo() {
        return githubRepo;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public String getBranch() {
        return branch;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public String getWorkerId() {
        return workerId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getLogTail() {
        return logTail;
    }

    public String getLogObjectKey() {
        return logObjectKey;
    }

    public Instant getHeartbeatAt() {
        return heartbeatAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
