package dev.forgeflow.api.pipeline.event;

import dev.forgeflow.common.event.PipelineQueuedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the internal {@link PipelineCreatedEvent} to the external pipeline.queued Kafka topic.
 * Listening AFTER_COMMIT means this only fires once the admitting transaction has durably
 * committed -- a worker that consumes pipeline.queued can always find the Pipeline row it
 * references.
 */
@Component
public class PipelineEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PipelineEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String pipelineQueuedTopic;

    public PipelineEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${forgeflow.kafka.topics.pipeline-queued:pipeline.queued}") String pipelineQueuedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.pipelineQueuedTopic = pipelineQueuedTopic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPipelineCreated(PipelineCreatedEvent event) {
        PipelineQueuedEvent payload = new PipelineQueuedEvent(
                event.pipelineId(), event.repositoryId(), event.githubOwner(), event.githubRepo(),
                event.commitSha(), event.branch(), event.occurredAt());

        kafkaTemplate.send(pipelineQueuedTopic, event.pipelineId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PipelineQueuedEvent for pipeline {}", event.pipelineId(), ex);
                    } else {
                        log.debug("Published PipelineQueuedEvent for pipeline {}", event.pipelineId());
                    }
                });
    }
}
