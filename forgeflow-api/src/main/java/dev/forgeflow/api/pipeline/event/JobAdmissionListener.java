package dev.forgeflow.api.pipeline.event;

import dev.forgeflow.api.pipeline.Pipeline;
import dev.forgeflow.api.pipeline.PipelineRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Transitions the newly created Pipeline from PENDING to QUEUED once its creating transaction
 * has committed, and increments the admission counter. Runs in its own transaction, separate
 * from {@link PipelineEventPublisher}, so a metrics/DB hiccup here can never block the Kafka
 * publish (or vice versa).
 */
@Component
public class JobAdmissionListener {

    private static final Logger log = LoggerFactory.getLogger(JobAdmissionListener.class);

    private final PipelineRepository pipelineRepository;
    private final MeterRegistry meterRegistry;

    public JobAdmissionListener(PipelineRepository pipelineRepository, MeterRegistry meterRegistry) {
        this.pipelineRepository = pipelineRepository;
        this.meterRegistry = meterRegistry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onPipelineCreated(PipelineCreatedEvent event) {
        pipelineRepository.findById(event.pipelineId()).ifPresentOrElse(pipeline -> {
            pipeline.markQueued();
            pipelineRepository.save(pipeline);
            meterRegistry.counter("forgeflow.pipelines.queued").increment();
        }, () -> log.warn("Pipeline {} vanished before admission could mark it QUEUED", event.pipelineId()));
    }
}
