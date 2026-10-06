package dev.forgeflow.worker.kafka;

import dev.forgeflow.common.event.PipelineFinishedEvent;
import dev.forgeflow.common.event.PipelineHeartbeatEvent;
import dev.forgeflow.common.event.PipelineQueuedEvent;
import dev.forgeflow.common.event.PipelineStartedEvent;
import dev.forgeflow.worker.job.ArtifactUploadService;
import dev.forgeflow.worker.job.DockerJobExecutor;
import dev.forgeflow.worker.job.JobExecutionException;
import dev.forgeflow.worker.job.WorkerIdentity;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Consumes pipeline.queued, claims the job under this worker's identity, runs it via
 * {@link DockerJobExecutor} and reports pipeline.started / pipeline.heartbeat / pipeline.finished
 * back to forgeflow-api. A background heartbeat is scheduled for the duration of the (blocking)
 * job execution so forgeflow-api's PipelineReaper can tell a live worker from a crashed one.
 */
@Component
public class PipelineQueueListener {

    private static final Logger log = LoggerFactory.getLogger(PipelineQueueListener.class);

    private final DockerJobExecutor jobExecutor;
    private final ArtifactUploadService artifactUploadService;
    private final WorkerIdentity workerIdentity;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String pipelineStartedTopic;
    private final String pipelineHeartbeatTopic;
    private final String pipelineFinishedTopic;
    private final long heartbeatIntervalSeconds;
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pipeline-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public PipelineQueueListener(
            DockerJobExecutor jobExecutor,
            ArtifactUploadService artifactUploadService,
            WorkerIdentity workerIdentity,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${forgeflow.kafka.topics.pipeline-started:pipeline.started}") String pipelineStartedTopic,
            @Value("${forgeflow.kafka.topics.pipeline-heartbeat:pipeline.heartbeat}") String pipelineHeartbeatTopic,
            @Value("${forgeflow.kafka.topics.pipeline-finished:pipeline.finished}") String pipelineFinishedTopic,
            @Value("${forgeflow.job.heartbeat-interval-seconds:20}") long heartbeatIntervalSeconds) {
        this.jobExecutor = jobExecutor;
        this.artifactUploadService = artifactUploadService;
        this.workerIdentity = workerIdentity;
        this.kafkaTemplate = kafkaTemplate;
        this.pipelineStartedTopic = pipelineStartedTopic;
        this.pipelineHeartbeatTopic = pipelineHeartbeatTopic;
        this.pipelineFinishedTopic = pipelineFinishedTopic;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    @KafkaListener(topics = "${forgeflow.kafka.topics.pipeline-queued:pipeline.queued}",
            groupId = "forgeflow-worker")
    public void onPipelineQueued(ConsumerRecord<String, PipelineQueuedEvent> record, Acknowledgment ack) {
        PipelineQueuedEvent event = record.value();
        log.info("Worker {} claimed pipeline {}", workerIdentity.id(), event.pipelineId());

        publishStarted(event);
        ScheduledFuture<?> heartbeat = scheduleHeartbeat(event);

        try {
            DockerJobExecutor.JobResult result = jobExecutor.execute(
                    event.pipelineId(), event.githubOwner(), event.githubRepo(), event.commitSha());
            String logObjectKey = artifactUploadService.uploadLog(event.pipelineId(), result.fullLog());
            publishFinished(event, result.succeeded(), result.exitCode(), result.logTail(), logObjectKey);
        } catch (JobExecutionException ex) {
            log.error("Job execution failed for pipeline {}", event.pipelineId(), ex);
            String logTail = "Job execution failed: " + ex.getMessage();
            String logObjectKey = artifactUploadService.uploadLog(event.pipelineId(), logTail);
            publishFinished(event, false, 1, logTail, logObjectKey);
        } finally {
            heartbeat.cancel(false);
            ack.acknowledge();
        }
    }

    private ScheduledFuture<?> scheduleHeartbeat(PipelineQueuedEvent event) {
        return heartbeatExecutor.scheduleAtFixedRate(
                () -> publishHeartbeat(event),
                heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
    }

    private void publishStarted(PipelineQueuedEvent event) {
        PipelineStartedEvent payload = new PipelineStartedEvent(event.pipelineId(), workerIdentity.id(), Instant.now());
        send(pipelineStartedTopic, event.pipelineId().toString(), payload);
    }

    private void publishHeartbeat(PipelineQueuedEvent event) {
        PipelineHeartbeatEvent payload = new PipelineHeartbeatEvent(event.pipelineId(), workerIdentity.id(), Instant.now());
        send(pipelineHeartbeatTopic, event.pipelineId().toString(), payload);
    }

    private void publishFinished(PipelineQueuedEvent event, boolean succeeded, int exitCode, String logTail, String logObjectKey) {
        PipelineFinishedEvent payload = new PipelineFinishedEvent(
                event.pipelineId(), succeeded, exitCode, logTail, logObjectKey, Instant.now());
        send(pipelineFinishedTopic, event.pipelineId().toString(), payload);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to {}", payload.getClass().getSimpleName(), topic, ex);
                    }
                });
    }
}
