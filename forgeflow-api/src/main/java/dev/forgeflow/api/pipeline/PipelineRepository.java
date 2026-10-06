package dev.forgeflow.api.pipeline;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PipelineRepository extends JpaRepository<Pipeline, UUID> {

    List<Pipeline> findByRepositoryIdOrderByCreatedAtDesc(UUID repositoryId);

    /**
     * Pipelines that are RUNNING but whose last heartbeat is older than the cutoff -- the worker
     * that owned them has presumably crashed or been killed without reporting completion.
     * Picked up by {@link PipelineReaper}.
     */
    @Query("select p from Pipeline p where p.status = dev.forgeflow.api.pipeline.PipelineStatus.RUNNING "
            + "and (p.heartbeatAt is null or p.heartbeatAt < :cutoff)")
    List<Pipeline> findStalledPipelines(@Param("cutoff") Instant cutoff);

    /** Pipelines whose retry delay has elapsed and are ready to be requeued. */
    @Query("select p from Pipeline p where p.status = dev.forgeflow.api.pipeline.PipelineStatus.RETRY_SCHEDULED "
            + "and p.nextRetryAt < :now")
    List<Pipeline> findDueForRetry(@Param("now") Instant now);
}
