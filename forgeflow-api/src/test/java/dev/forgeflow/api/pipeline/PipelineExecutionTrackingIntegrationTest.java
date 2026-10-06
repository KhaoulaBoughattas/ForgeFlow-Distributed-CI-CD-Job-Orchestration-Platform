package dev.forgeflow.api.pipeline;

import dev.forgeflow.api.AbstractIntegrationTest;
import dev.forgeflow.common.event.PipelineFinishedEvent;
import dev.forgeflow.common.event.PipelineStartedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifies PipelineExecutionListener correctly projects worker-originated Kafka events
 * (pipeline.started / pipeline.finished) onto a Pipeline row, independent of the worker module
 * -- we publish the events directly with a KafkaTemplate to isolate the API's consumption path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AbstractIntegrationTest.class)
class PipelineExecutionTrackingIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PipelineRepository pipelineRepository;

    @Value("${forgeflow.kafka.topics.pipeline-started:pipeline.started}")
    private String pipelineStartedTopic;

    @Value("${forgeflow.kafka.topics.pipeline-finished:pipeline.finished}")
    private String pipelineFinishedTopic;

    @Test
    void startedAndFinishedEventsUpdateThePipelineRow() {
        Pipeline pipeline = Pipeline.createPending(UUID.randomUUID(), "octocat", "exec-tracking-repo", "cafebabecafebabecafebabecafebabecafebabe", "main");
        pipelineRepository.save(pipeline);

        kafkaTemplate.send(pipelineStartedTopic, pipeline.getId().toString(),
                new PipelineStartedEvent(pipeline.getId(), "worker-test-1", Instant.now()));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Pipeline reloaded = pipelineRepository.findById(pipeline.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(PipelineStatus.RUNNING);
            assertThat(reloaded.getWorkerId()).isEqualTo("worker-test-1");
        });

        kafkaTemplate.send(pipelineFinishedTopic, pipeline.getId().toString(),
                new PipelineFinishedEvent(pipeline.getId(), true, 0, "build succeeded", "logs/" + pipeline.getId() + ".log", Instant.now()));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Pipeline reloaded = pipelineRepository.findById(pipeline.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(PipelineStatus.SUCCEEDED);
            assertThat(reloaded.getExitCode()).isEqualTo(0);
            assertThat(reloaded.getLogObjectKey()).isNotBlank();
        });
    }
}
