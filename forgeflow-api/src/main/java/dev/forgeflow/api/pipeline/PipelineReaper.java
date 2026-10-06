package dev.forgeflow.api.pipeline;

import dev.forgeflow.common.event.PipelineQueuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Periodically sweeps for pipelines whose worker has gone silent (no heartbeat within the
 * staleness window) and either schedules a retry or marks them permanently failed once retries
 * are exhausted. Also requeues pipelines whose retry delay has elapsed, republishing them to
 * Kafka so a (possibly different) worker picks them up. Disabled in tests via
 * forgeflow.pipeline.reaper-enabled=false, since the short staleness window would otherwise
 * make slow CI runs flaky.
 */
@Component
public class PipelineReaper {

    private static final Logger log = LoggerFactory.getLogger(PipelineReaper.class);
    private static final int MAX_RETRIES = 3;

    private final PipelineRepository pipelineRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String pipelineQueuedTopic;
    private final Duration staleAfter;
    private final Duration retryDelay;
    private final boolean enabled;

    public PipelineReaper(
            PipelineRepository pipelineRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${forgeflow.kafka.topics.pipeline-queued:pipeline.queued}") String pipelineQueuedTopic,
            @Value("${forgeflow.pipeline.heartbeat-stale-after-seconds:90}") long staleAfterSeconds,
            @Value("${forgeflow.pipeline.retry-delay-seconds:30}") long retryDelaySeconds,
            @Value("${forgeflow.pipeline.reaper-enabled:true}") boolean enabled) {
        this.pipelineRepository = pipelineRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.pipelineQueuedTopic = pipelineQueuedTopic;
        this.staleAfter = Duration.ofSeconds(staleAfterSeconds);
        this.retryDelay = Duration.ofSeconds(retryDelaySeconds);
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${forgeflow.pipeline.reaper-interval-ms:15000}")
    @Transactional
    public void reapStalledPipelines() {
        if (!enabled) {
            return;
        }

        Instant cutoff = Instant.now().minus(staleAfter);
        List<Pipeline> stalled = pipelineRepository.findStalledPipelines(cutoff);
        for (Pipeline pipeline : stalled) {
            if (pipeline.getRetryCount() < MAX_RETRIES) {
                log.warn("Pipeline {} stalled (worker {}), scheduling retry {}/{}",
                        pipeline.getId(), pipeline.getWorkerId(), pipeline.getRetryCount() + 1, MAX_RETRIES);
                pipeline.scheduleRetry(retryDelay);
            } else {
                log.error("Pipeline {} stalled and exhausted {} retries, marking FAILED",
                        pipeline.getId(), MAX_RETRIES);
                pipeline.markFailedExhausted();
            }
            pipelineRepository.save(pipeline);
        }

        List<Pipeline> dueForRetry = pipelineRepository.findDueForRetry(Instant.now());
        for (Pipeline pipeline : dueForRetry) {
            pipeline.requeueAfterRetryDelay();
            pipelineRepository.save(pipeline);

            PipelineQueuedEvent payload = new PipelineQueuedEvent(
                    pipeline.getId(), pipeline.getRepositoryId(), pipeline.getGithubOwner(), pipeline.getGithubRepo(),
                    pipeline.getCommitSha(), pipeline.getBranch(), Instant.now());
            kafkaTemplate.send(pipelineQueuedTopic, pipeline.getId().toString(), payload);
            log.info("Requeued pipeline {} for retry attempt {}", pipeline.getId(), pipeline.getRetryCount());
        }
    }
}
