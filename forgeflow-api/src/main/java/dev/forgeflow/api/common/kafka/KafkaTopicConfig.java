package dev.forgeflow.api.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares the Kafka topics used by the pipeline event pipeline, plus a dead-letter topic for
 * each, consumed by {@link KafkaErrorHandlingConfig}'s DeadLetterPublishingRecoverer after
 * retries are exhausted.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${forgeflow.kafka.topics.pipeline-queued:pipeline.queued}")
    private String pipelineQueuedTopic;

    @Value("${forgeflow.kafka.topics.pipeline-started:pipeline.started}")
    private String pipelineStartedTopic;

    @Value("${forgeflow.kafka.topics.pipeline-heartbeat:pipeline.heartbeat}")
    private String pipelineHeartbeatTopic;

    @Value("${forgeflow.kafka.topics.pipeline-finished:pipeline.finished}")
    private String pipelineFinishedTopic;

    @Bean
    public KafkaAdmin.NewTopics pipelineTopics() {
        return new KafkaAdmin.NewTopics(
                topic(pipelineQueuedTopic),
                topic(pipelineQueuedTopic + ".DLT"),
                topic(pipelineStartedTopic),
                topic(pipelineStartedTopic + ".DLT"),
                topic(pipelineHeartbeatTopic),
                topic(pipelineHeartbeatTopic + ".DLT"),
                topic(pipelineFinishedTopic),
                topic(pipelineFinishedTopic + ".DLT")
        );
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
