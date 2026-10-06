package dev.forgeflow.api.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineReaperTest {

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void stalledPipelineUnderRetryLimitIsScheduledForRetry() {
        Pipeline pipeline = Pipeline.createPending(UUID.randomUUID(), "octocat", "reaper-repo", "a".repeat(40), "main");
        pipeline.markRunning("worker-stale-1");

        when(pipelineRepository.findStalledPipelines(any())).thenReturn(List.of(pipeline));
        when(pipelineRepository.findDueForRetry(any())).thenReturn(List.of());

        PipelineReaper reaper = new PipelineReaper(pipelineRepository, kafkaTemplate, "pipeline.queued", 90, 30, true);
        reaper.reapStalledPipelines();

        assertThat(pipeline.getStatus()).isEqualTo(PipelineStatus.RETRY_SCHEDULED);
        assertThat(pipeline.getRetryCount()).isEqualTo(1);
    }

    @Test
    void stalledPipelineThatExhaustedRetriesIsMarkedFailed() {
        Pipeline pipeline = Pipeline.createPending(UUID.randomUUID(), "octocat", "reaper-repo", "b".repeat(40), "main");
        pipeline.markRunning("worker-stale-2");
        pipeline.scheduleRetry(java.time.Duration.ZERO);
        pipeline.scheduleRetry(java.time.Duration.ZERO);
        pipeline.scheduleRetry(java.time.Duration.ZERO);

        when(pipelineRepository.findStalledPipelines(any())).thenReturn(List.of(pipeline));
        when(pipelineRepository.findDueForRetry(any())).thenReturn(List.of());

        PipelineReaper reaper = new PipelineReaper(pipelineRepository, kafkaTemplate, "pipeline.queued", 90, 30, true);
        reaper.reapStalledPipelines();

        assertThat(pipeline.getStatus()).isEqualTo(PipelineStatus.FAILED);
    }

    @Test
    void disabledReaperDoesNothing() {
        PipelineReaper reaper = new PipelineReaper(pipelineRepository, kafkaTemplate, "pipeline.queued", 90, 30, false);
        reaper.reapStalledPipelines();
        org.mockito.Mockito.verifyNoInteractions(pipelineRepository);
    }
}
