package dev.forgeflow.api.pipeline.event;

import dev.forgeflow.api.pipeline.Pipeline;
import dev.forgeflow.api.pipeline.PipelineRepository;
import dev.forgeflow.common.event.PipelineFinishedEvent;
import dev.forgeflow.common.event.PipelineHeartbeatEvent;
import dev.forgeflow.common.event.PipelineStartedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes the worker-originated lifecycle events (pipeline.started/heartbeat/finished) and
 * projects them onto the Pipeline row that forgeflow-api owns. Uses MANUAL_IMMEDIATE
 * acknowledgment (see application.yml listener.ack-mode) so a message is only committed once
 * its DB update has actually succeeded.
 */
@Component
public class PipelineExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(PipelineExecutionListener.class);

    private final PipelineRepository pipelineRepository;
    private final MeterRegistry meterRegistry;

    public PipelineExecutionListener(PipelineRepository pipelineRepository, MeterRegistry meterRegistry) {
        this.pipelineRepository = pipelineRepository;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = "${forgeflow.kafka.topics.pipeline-started:pipeline.started}",
            groupId = "forgeflow-api-execution-tracking")
    @Transactional
    public void onStarted(ConsumerRecord<String, PipelineStartedEvent> record, Acknowledgment ack) {
        PipelineStartedEvent event = record.value();
        pipelineRepository.findById(event.pipelineId()).ifPresentOrElse(pipeline -> {
            pipeline.markRunning(event.workerId());
            pipelineRepository.save(pipeline);
        }, () -> log.warn("Received PipelineStartedEvent for unknown pipeline {}", event.pipelineId()));
        ack.acknowledge();
    }

    @KafkaListener(topics = "${forgeflow.kafka.topics.pipeline-heartbeat:pipeline.heartbeat}",
            groupId = "forgeflow-api-execution-tracking")
    @Transactional
    public void onHeartbeat(ConsumerRecord<String, PipelineHeartbeatEvent> record, Acknowledgment ack) {
        PipelineHeartbeatEvent event = record.value();
        pipelineRepository.findById(event.pipelineId()).ifPresent(pipeline -> {
            pipeline.recordHeartbeat();
            pipelineRepository.save(pipeline);
        });
        ack.acknowledge();
    }

    @KafkaListener(topics = "${forgeflow.kafka.topics.pipeline-finished:pipeline.finished}",
            groupId = "forgeflow-api-execution-tracking")
    @Transactional
    public void onFinished(ConsumerRecord<String, PipelineFinishedEvent> record, Acknowledgment ack) {
        PipelineFinishedEvent event = record.value();
        pipelineRepository.findById(event.pipelineId()).ifPresentOrElse(pipeline -> {
            pipeline.markFinished(event.succeeded(), event.exitCode(), event.logTail(), event.logObjectKey());
            pipelineRepository.save(pipeline);
            meterRegistry.counter(event.succeeded() ? "forgeflow.pipelines.succeeded" : "forgeflow.pipelines.failed")
                    .increment();
        }, () -> log.warn("Received PipelineFinishedEvent for unknown pipeline {}", event.pipelineId()));
        ack.acknowledge();
    }
}
